package com.iki.ps3player

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.StatFs
import android.provider.OpenableColumns
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.iki.ps3player.core.CoreManager
import com.iki.ps3player.diagnostics.DeviceDiagnostics
import com.iki.ps3player.emulator.BackendFactory
import com.iki.ps3player.emulator.CoreInstaller
import com.iki.ps3player.emulator.EmulatorManager
import com.iki.ps3player.emulator.RpcsxBackend
import com.iki.ps3player.game.GameEntry
import com.iki.ps3player.game.GameValidator
import com.iki.ps3player.storage.AppStorage
import com.iki.ps3player.ui.*
import kotlinx.coroutines.launch
import net.rpcsx.CoreEvents
import java.io.File

sealed interface Screen {
    object Home : Screen
    object Diag : Screen
    object Setup : Screen
    data class Detail(val id: String) : Screen
    data class Player(val id: String, val edit: Boolean) : Screen
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(colorScheme = darkColorScheme()) {
                Surface(Modifier.fillMaxSize()) { App(remember { AppStorage(applicationContext) }) }
            }
        }
    }
}

private fun queryMeta(ctx: Context, uri: Uri): Pair<String, Long> {
    var name = uri.lastPathSegment ?: "unknown"
    var size = 0L
    ctx.contentResolver.query(uri, null, null, null, null)?.use { c ->
        if (c.moveToFirst()) {
            c.getColumnIndex(OpenableColumns.DISPLAY_NAME).takeIf { it >= 0 }?.let { name = c.getString(it) }
            c.getColumnIndex(OpenableColumns.SIZE).takeIf { it >= 0 }?.let { size = c.getLong(it) }
        }
    }
    return name to size
}

@Composable
fun App(storage: AppStorage) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var manager by remember { mutableStateOf(EmulatorManager(BackendFactory.create(ctx.applicationContext))) }
    val installer = manager.backend as? CoreInstaller
    val games = remember { mutableStateListOf<GameEntry>().apply { addAll(storage.loadGames()) } }
    var screen by remember { mutableStateOf<Screen>(Screen.Home) }
    var error by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf<String?>(null) }
    var message by remember { mutableStateOf<String?>(null) }
    var coreProgress by remember { mutableStateOf<String?>(null) }
    val progress by CoreEvents.progress.collectAsState()
    var fwStatus by remember { mutableStateOf(storage.firmwareStatus()) }

    val busyText = busy?.let { b -> progress?.takeIf { it.second > 0 && it.first >= 0 }?.let { "$b ${it.first * 100 / it.second}%" } ?: b }
        ?: coreProgress

    fun addEntry(g: GameEntry) { if (games.none { it.id == g.id }) games.add(g) }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        val (name, size) = queryMeta(ctx, uri)
        val valid = GameValidator.validate(name, size)
        if (valid.isFailure) { error = valid.exceptionOrNull()?.message; return@rememberLauncherForActivityResult }
        error = null
        if (installer == null) { // mode mock: hanya mendaftarkan URI
            runCatching { ctx.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION) }
            addEntry(GameEntry(uri.toString().hashCode().toString(), name, uri.toString(), size))
            storage.saveGames(games.toList())
        } else scope.launch { // mode nyata: core memasang game ke penyimpanan app
            val free = StatFs(ctx.getExternalFilesDir(null)!!.path).availableBytes
            if (free < size + (512L shl 20)) { error = "Ruang kosong tidak cukup untuk memasang $name."; return@launch }
            busy = "Memasang $name (bisa beberapa menit)"
            val r = runCatching { installer.installGame(uri).getOrThrow(); installer.scanGames() }
            busy = null
            r.onSuccess { list ->
                list.forEach { g ->
                    val f = File(g.path)
                    addEntry(GameEntry(g.path.hashCode().toString(), g.name ?: f.name, Uri.fromFile(f).toString(), if (f.isFile) f.length() else 0L))
                }
                storage.saveGames(games.toList())
                if (list.isEmpty()) error = "Game terpasang tetapi tidak terdeteksi oleh pemindaian."
            }.onFailure { error = it.message }
        }
    }

    val fwPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null || installer == null) return@rememberLauncherForActivityResult
        scope.launch {
            busy = "Memasang firmware (bisa beberapa menit)"; message = null
            val r = installer.installFirmware(uri)
            busy = null
            r.onSuccess { fwStatus = "Terpasang" + (CoreEvents.firmware.value?.let { " ($it)" } ?: ""); storage.setFirmwareStatus(fwStatus!!); message = "Firmware terpasang." }
                .onFailure { message = "Gagal: ${it.message}" }
        }
    }

    BackHandler(screen != Screen.Home) {
        val s = screen
        screen = if (s is Screen.Player) Screen.Detail(s.id) else Screen.Home
    }

    when (val s = screen) {
        Screen.Home -> HomeScreen(
            games, error, busyText, manager.backend.capabilities.name,
            onAdd = { picker.launch(arrayOf("*/*")) }, onOpen = { screen = Screen.Detail(it.id) },
            onDiag = { screen = Screen.Diag }, onSetup = { screen = Screen.Setup },
        )
        Screen.Diag -> DiagnosticsScreen(remember { DeviceDiagnostics.collect(ctx) }) { screen = Screen.Home }
        Screen.Setup -> {
            val core = CoreManager.coreFile(ctx)
            SetupScreen(
                backendName = manager.backend.capabilities.name,
                glueOk = RpcsxBackend.isGlueAvailable(),
                coreText = if (core.exists()) "Terpasang (${core.length() shr 20} MB)" else "Belum terpasang",
                fwText = fwStatus ?: "Belum terpasang",
                working = busyText, message = message,
                onDownloadCore = {
                    scope.launch {
                        message = null; coreProgress = "Mengunduh core…"
                        CoreManager.download(ctx) { d, t -> coreProgress = "Mengunduh core… ${if (t > 0) "${d * 100 / t}%" else "${d shr 20} MB"}" }
                            .onSuccess { label ->
                                manager = EmulatorManager(BackendFactory.create(ctx.applicationContext))
                                message = "Core terpasang: $label"
                            }.onFailure { message = "Gagal mengunduh core: ${it.message}" }
                        coreProgress = null
                    }
                },
                onPickFirmware = { fwPicker.launch(arrayOf("*/*")) },
                onBack = { screen = Screen.Home },
            )
        }
        is Screen.Detail -> {
            val g = games.firstOrNull { it.id == s.id }
            if (g == null) screen = Screen.Home else DetailScreen(
                g, manager.backend.capabilities, manager.backend.getRuntimeDiagnostics().log.takeLast(8),
                onPlay = { screen = Screen.Player(g.id, false) },
                onControls = { screen = Screen.Player(g.id, true) },
                onRemove = { games.remove(g); storage.saveGames(games.toList()); storage.deleteLayout(g.id); screen = Screen.Home },
                onBack = { screen = Screen.Home },
            )
        }
        is Screen.Player -> {
            val g = games.firstOrNull { it.id == s.id }
            if (g == null) screen = Screen.Home
            else PlayerScreen(g, manager, storage, s.edit) { screen = Screen.Detail(g.id) }
        }
    }
}
