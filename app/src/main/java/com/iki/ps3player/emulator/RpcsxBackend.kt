package com.iki.ps3player.emulator

import android.content.Context
import android.net.Uri
import android.view.Surface
import com.iki.ps3player.controls.InputState
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import net.rpcsx.CoreEvents
import net.rpcsx.RPCSX
import java.io.File
import kotlin.concurrent.thread

/**
 * Adapter untuk core RPCSX-Clanker lewat binding JNI net.rpcsx.RPCSX (GPLv2).
 * Urutan pemanggilan mengikuti MainActivity/RPCSXActivity di rpcsx-ui-android.
 * BELUM DIUJI di perangkat. Pause memakai openHomeMenu() karena tidak ada JNI pause khusus.
 */
class RpcsxBackend(private val ctx: Context, private val corePath: String, private val user: String = "00000001") : EmulatorBackend, CoreInstaller {
    override val capabilities = BackendCapabilities("RPCSX-Clanker core (eksperimental)", isMock = false)
    private val _state = MutableStateFlow(SessionState.IDLE)
    override val state: StateFlow<SessionState> = _state.asStateFlow()
    private val log = ArrayDeque<String>()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var poller: Job? = null
    private var initialized = false
    private val initMutex = Mutex()
    private var rootDir = ""
    private var pendingPath: String? = null
    @Volatile private var bootError: String? = null

    private fun note(m: String) = synchronized(log) { log.addLast(m); while (log.size > 100) log.removeFirst() }
    private val rpcsx get() = RPCSX.instance

    override suspend fun initialize(): Result<Unit> = withContext(Dispatchers.IO) {
        initMutex.withLock {
            runCatching {
                if (initialized) return@runCatching
                check(rpcsx.openLibrary(corePath)) { "Core tidak bisa dibuka: $corePath" }
                rootDir = ctx.getExternalFilesDir(null)!!.absolutePath.let { if (it.endsWith("/")) it else "$it/" }
                check(rpcsx.initialize(rootDir, user)) { "initialize() gagal" }
                thread(name = "rpcsx-main") { rpcsx.startMainThreadProcessor() }
                thread(name = "rpcsx-compile") { rpcsx.processCompilationQueue() }
                initialized = true
                note("core siap ($corePath)")
            }.onFailure { note("initialize gagal: ${it.message}") }
        }
    }

    private fun installFd(uri: Uri, fw: Boolean, id: Long) {
        CoreEvents.progress.value = null
        val afd = ctx.contentResolver.openAssetFileDescriptor(uri, "r") ?: error("File tidak bisa dibuka")
        afd.use {
            val fd = it.parcelFileDescriptor.fd
            val ok = if (fw) rpcsx.installFw(fd, id) else rpcsx.install(fd, id)
            check(ok) { if (fw) "Pemasangan firmware gagal (pastikan file PS3UPDAT.PUP resmi)" else "Pemasangan game gagal" }
        }
    }

    override suspend fun installFirmware(uri: Uri): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching { initialize().getOrThrow(); installFd(uri, true, 1L); note("firmware terpasang") }
            .onFailure { note("installFirmware: ${it.message}") }
    }

    override suspend fun installGame(uri: Uri): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching { initialize().getOrThrow(); installFd(uri, false, 2L); note("game terpasang") }
            .onFailure { note("installGame: ${it.message}") }
    }

    /** Hasil pemindaian datang lewat callback GameRepository.add; jeda singkat memberi waktu callback masuk. */
    override suspend fun scanGames(): List<InstalledGame> = withContext(Dispatchers.IO) {
        initialize().getOrThrow()
        CoreEvents.games.value = emptyList()
        rpcsx.collectGameInfo(rootDir + "config/dev_hdd0/game", -1L)
        rpcsx.collectGameInfo(rootDir + "config/games", -1L)
        delay(1500)
        CoreEvents.games.value.map { InstalledGame(it.path, it.name) }
    }

    /** path harus berupa path file/folder nyata (core tidak menerima content://). */
    override suspend fun loadGame(uri: Uri): Result<Unit> {
        val path = uri.path?.takeIf { uri.scheme == "file" }
            ?: return Result.failure(IllegalArgumentException(
                "Core membutuhkan path file nyata, bukan content://. Salin game ke penyimpanan app/folder terbuka dahulu (belum diimplementasikan)."))
        pendingPath = path; _state.value = SessionState.READY; return Result.success(Unit)
    }

    override fun startSession() {
        val path = pendingPath ?: return
        bootError = null; _state.value = SessionState.LOADING
        startPolling()
        thread(name = "rpcsx-boot") {
            val code = rpcsx.boot(path) // memblokir; boot pertama bisa memakan menit (kompilasi PPU)
            if (code != 0) { bootError = BOOT_RESULTS.getOrElse(code) { "Kode $code" }; note("boot gagal: $bootError"); _state.value = SessionState.ERROR }
            else note("boot selesai")
        }
    }

    private fun startPolling() {
        poller?.cancel()
        poller = scope.launch {
            while (isActive) {
                if (_state.value != SessionState.ERROR) _state.value = when (rpcsx.getState()) {
                    1, 7 -> SessionState.LOADING          // Loading, Starting
                    2 -> SessionState.STOPPING
                    3 -> SessionState.RUNNING
                    4, 5 -> SessionState.PAUSED           // Paused, Frozen (Frozen tidak bisa resume)
                    6 -> SessionState.READY
                    else -> if (_state.value == SessionState.LOADING) SessionState.LOADING else SessionState.STOPPED
                }
                delay(500)
            }
        }
    }

    override fun pauseSession() { runCatching { rpcsx.openHomeMenu(); note("pause (menu PS)") } }
    override fun resumeSession() { runCatching { rpcsx.resume(); note("resume") } }
    override fun stopSession() { runCatching { rpcsx.kill(); note("kill") }; poller?.cancel() }

    override fun sendControllerInput(input: InputState) {
        val p = RpcsxPadMapper.map(input)
        runCatching { rpcsx.overlayPadData(p.d1, p.d2, p.lx, p.ly, p.rx, p.ry) }
    }

    override fun onSurfaceEvent(surface: Surface, event: SurfaceEvent) {
        runCatching { rpcsx.surfaceEvent(surface, event.ordinal) }
    }

    override fun getRuntimeDiagnostics(): RuntimeDiagnostics {
        val work = runCatching { rpcsx.getFrameWorkNanos() }.getOrDefault(0L)
        val period = runCatching { rpcsx.getFramePeriodNanos() }.getOrDefault(0L)
        return RuntimeDiagnostics(
            emulationFps = if (period > 0) 1e9f / period else null,
            frameTimeMs = if (work > 0) work / 1e6f else null,
            log = synchronized(log) { log.toList() },
        )
    }

    companion object {
        /** Urutan sama dengan enum BootResult di rpcsx-ui-android. */
        val BOOT_RESULTS = listOf("NoErrors", "GenericError", "NothingToBoot", "WrongDiscLocation", "InvalidFileOrFolder",
            "InvalidBDvdFolder", "InstallFailed", "DecryptionError", "FileCreationError", "FirmwareMissing",
            "UnsupportedDiscType", "SavestateCorrupted", "SavestateVersionUnsupported", "StillRunning", "AlreadyAdded", "CurrentlyRestricted")

        /** True hanya jika native glue (libnative "rpcsx-android") ikut ter-build dan termuat. */
        fun isGlueAvailable(): Boolean = runCatching { RPCSX.instance; true }.getOrDefault(false)
    }
}
