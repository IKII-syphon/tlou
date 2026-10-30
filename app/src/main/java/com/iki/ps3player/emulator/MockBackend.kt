package com.iki.ps3player.emulator

import android.net.Uri
import android.view.Surface
import com.iki.ps3player.controls.InputState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Backend palsu: hanya memvalidasi lifecycle & mencatat input. TIDAK menjalankan game. */
class MockBackend : EmulatorBackend {
    override val capabilities = BackendCapabilities("Mock backend (tanpa emulasi)", isMock = true)
    private val _state = MutableStateFlow(SessionState.IDLE)
    override val state: StateFlow<SessionState> = _state.asStateFlow()
    private val log = ArrayDeque<String>()
    private var lastButtons = 0

    private fun note(m: String) = synchronized(log) {
        log.addLast(m); while (log.size > 100) log.removeFirst()
    }

    private fun go(from: Set<SessionState>, to: SessionState, label: String) {
        if (_state.value in from) { _state.value = to; note(label) }
        else note("$label diabaikan (state ${_state.value})")
    }

    override suspend fun initialize(): Result<Unit> { note("initialize"); return Result.success(Unit) }
    override suspend fun loadGame(uri: Uri): Result<Unit> {
        note("loadGame $uri"); _state.value = SessionState.READY; return Result.success(Unit)
    }
    override fun startSession() = go(setOf(SessionState.READY, SessionState.STOPPED), SessionState.RUNNING, "start")
    override fun pauseSession() = go(setOf(SessionState.RUNNING), SessionState.PAUSED, "pause")
    override fun resumeSession() = go(setOf(SessionState.PAUSED), SessionState.RUNNING, "resume")
    override fun stopSession() = go(
        setOf(SessionState.RUNNING, SessionState.PAUSED, SessionState.READY), SessionState.STOPPED, "stop",
    )

    override fun sendControllerInput(input: InputState) {
        if (_state.value != SessionState.RUNNING) return
        if (input.buttons != lastButtons) { lastButtons = input.buttons; note("input buttons=0x%X".format(input.buttons)) }
    }

    override fun onSurfaceEvent(surface: Surface, event: SurfaceEvent) = Unit

    override fun getRuntimeDiagnostics() =
        RuntimeDiagnostics(null, null, synchronized(log) { log.toList() })
}
