package com.iki.ps3player.emulator

import android.net.Uri
import android.view.Surface
import com.iki.ps3player.controls.InputState
import kotlinx.coroutines.flow.StateFlow

enum class SessionState { IDLE, LOADING, READY, RUNNING, PAUSED, STOPPING, STOPPED, ERROR }

enum class SurfaceEvent { CREATED, CHANGED, DESTROYED }

/** Opsi UI hanya ditampilkan bila flag terkait bernilai true. */
data class BackendCapabilities(
    val name: String,
    val isMock: Boolean,
    val supportsVulkan: Boolean = false,
    val supportsResolutionScale: Boolean = false,
    val supportsFrameLimiter: Boolean = false,
    val supportsExternalController: Boolean = false,
)

/** Null = backend tidak menyediakan nilai tersebut (jangan dipalsukan). */
data class RuntimeDiagnostics(val emulationFps: Float?, val frameTimeMs: Float?, val log: List<String>)

/**
 * Kontrak rancangan aplikasi (bukan API emulator tertentu).
 * Backend nyata harus mengimplementasikan interface ini sesuai API aslinya.
 */
interface EmulatorBackend {
    val capabilities: BackendCapabilities
    val state: StateFlow<SessionState>
    suspend fun initialize(): Result<Unit>
    suspend fun loadGame(uri: Uri): Result<Unit>
    fun startSession()
    fun pauseSession()
    fun resumeSession()
    fun stopSession()
    fun sendControllerInput(input: InputState)
    /** Surface render dari UI; backend nyata meneruskannya ke core. */
    fun onSurfaceEvent(surface: Surface, event: SurfaceEvent)
    fun getRuntimeDiagnostics(): RuntimeDiagnostics
}
