package com.iki.ps3player.ui

import android.app.Activity
import android.net.Uri
import android.view.WindowManager
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import android.view.SurfaceHolder
import android.view.SurfaceView
import com.iki.ps3player.emulator.SurfaceEvent
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.iki.ps3player.controls.ControlLayout
import com.iki.ps3player.controls.InputState
import com.iki.ps3player.controls.VirtualControls
import com.iki.ps3player.emulator.EmulatorManager
import com.iki.ps3player.emulator.SessionState
import com.iki.ps3player.game.GameEntry
import com.iki.ps3player.storage.AppStorage

private class InputHolder { var v = InputState() }

@Composable
fun PlayerScreen(game: GameEntry, manager: EmulatorManager, storage: AppStorage, startInEdit: Boolean, onExit: () -> Unit) {
    val backend = manager.backend
    val state by backend.state.collectAsState()
    var layout by remember { mutableStateOf(storage.loadLayout(game.id)) }
    val latestLayout by rememberUpdatedState(layout)
    var edit by remember { mutableStateOf(startInEdit) }
    var showControls by remember { mutableStateOf(true) }
    var showPerf by remember { mutableStateOf(false) }
    var menu by remember { mutableStateOf(false) }
    var input by remember { mutableStateOf(InputState()) }
    var err by remember { mutableStateOf<String?>(null) }
    var uiFps by remember { mutableStateOf(0) }
    val holder = remember { InputHolder() }

    fun update(f: (InputState) -> InputState) {
        holder.v = f(holder.v); input = holder.v; backend.sendControllerInput(holder.v)
    }

    // Sesi: siapkan & mulai; hentikan saat keluar.
    LaunchedEffect(game.id) {
        manager.prepare(Uri.parse(game.uri)).onSuccess { backend.startSession() }.onFailure { err = it.message }
    }
    DisposableEffect(Unit) { onDispose { backend.stopSession(); storage.saveLayout(game.id, latestLayout) } }

    // Pause/resume otomatis mengikuti lifecycle Activity.
    val owner = LocalLifecycleOwner.current
    DisposableEffect(owner) {
        var pausedByLifecycle = false
        val obs = LifecycleEventObserver { _, e ->
            if (e == Lifecycle.Event.ON_PAUSE && backend.state.value == SessionState.RUNNING) { backend.pauseSession(); pausedByLifecycle = true }
            if (e == Lifecycle.Event.ON_RESUME && pausedByLifecycle) { backend.resumeSession(); pausedByLifecycle = false }
        }
        owner.lifecycle.addObserver(obs)
        onDispose { owner.lifecycle.removeObserver(obs) }
    }

    // Layar penuh + layar tetap menyala.
    val activity = LocalContext.current as Activity
    DisposableEffect(Unit) {
        val c = WindowCompat.getInsetsController(activity.window, activity.window.decorView)
        c.hide(WindowInsetsCompat.Type.systemBars())
        c.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        activity.window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        onDispose {
            c.show(WindowInsetsCompat.Type.systemBars())
            activity.window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    // FPS UI aplikasi (BUKAN fps emulasi).
    LaunchedEffect(showPerf) {
        if (!showPerf) return@LaunchedEffect
        var last = 0L; var frames = 0
        while (true) withFrameNanos { t ->
            if (last == 0L) last = t
            frames++
            if (t - last >= 1_000_000_000L) { uiFps = frames; frames = 0; last = t }
        }
    }

    Box(Modifier.fillMaxSize().background(Color.Black)) {
        if (!backend.capabilities.isMock) {
            AndroidView(Modifier.fillMaxSize(), factory = { c ->
                SurfaceView(c).apply {
                    holder.addCallback(object : SurfaceHolder.Callback {
                        override fun surfaceCreated(h: SurfaceHolder) = backend.onSurfaceEvent(h.surface, SurfaceEvent.CREATED)
                        override fun surfaceChanged(h: SurfaceHolder, f: Int, w: Int, hh: Int) = backend.onSurfaceEvent(h.surface, SurfaceEvent.CHANGED)
                        override fun surfaceDestroyed(h: SurfaceHolder) = backend.onSurfaceEvent(h.surface, SurfaceEvent.DESTROYED)
                    })
                }
            })
        }
        Column(Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
            if (backend.capabilities.isMock) Text("Mock backend: tidak ada render game", color = Color.Gray)
            Text("Sesi: $state", color = Color.Gray)
            err?.let { Text(it, color = Color.Red) }
            Text(
                "Input terkirim: btn=0x%X  L(%.2f, %.2f)  R(%.2f, %.2f)".format(input.buttons, input.lx, input.ly, input.rx, input.ry),
                color = Color.DarkGray,
            )
            if (showPerf) {
                val d = backend.getRuntimeDiagnostics()
                Text(
                    "UI FPS: $uiFps (bukan FPS emulasi) • FPS emulasi: ${d.emulationFps ?: "n/a"} • frame time: ${d.frameTimeMs ?: "n/a"}",
                    color = Color.DarkGray,
                )
            }
        }

        if (showControls) {
            VirtualControls(
                layout = layout, edit = edit,
                onMove = { id, dx, dy -> layout = layout.moved(id, dx, dy) },
                onInput = { f -> update(f) },
            )
        }

        Row(Modifier.align(Alignment.TopCenter).padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            val running = state == SessionState.RUNNING
            FilledTonalButton(
                onClick = { if (running) backend.pauseSession() else backend.resumeSession() },
                enabled = running || state == SessionState.PAUSED,
            ) { Text(if (running) "Pause" else "Resume") }
            FilledTonalButton(onClick = { showControls = !showControls; update { InputState() } }) { Text(if (showControls) "Sembunyikan" else "Tampilkan") }
            FilledTonalButton(onClick = { menu = true }) { Text("Menu") }
        }
    }

    if (menu) {
        AlertDialog(
            onDismissRequest = { menu = false; storage.saveLayout(game.id, layout) },
            confirmButton = { TextButton({ menu = false; storage.saveLayout(game.id, layout) }) { Text("Tutup") } },
            dismissButton = { TextButton({ menu = false; onExit() }) { Text("Keluar") } },
            title = { Text("Menu cepat") },
            text = {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    Labeled("Transparansi", layout.alpha, 0.1f..1f) { layout = layout.copy(alpha = it) }
                    Labeled("Ukuran kontrol", layout.scale, 0.6f..1.6f) { layout = layout.copy(scale = it) }
                    Labeled("Dead zone analog", layout.deadZone, 0f..0.5f) { layout = layout.copy(deadZone = it) }
                    Labeled("Sensitivitas kamera", layout.cameraSensitivity, 0.5f..2f) { layout = layout.copy(cameraSensitivity = it) }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton({ layout = layout.withPreset(16f / 9f) }) { Text("Preset 16:9") }
                        OutlinedButton({ layout = layout.withPreset(20f / 9f) }) { Text("Preset 20:9") }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Switch(edit, { edit = it; update { InputState() } }); Text("  Mode edit posisi")
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Switch(showPerf, { showPerf = it }); Text("  Indikator performa")
                    }
                }
            },
        )
    }
}

@Composable
private fun Labeled(label: String, value: Float, range: ClosedFloatingPointRange<Float>, onChange: (Float) -> Unit) {
    Text("$label: %.2f".format(value), style = MaterialTheme.typography.bodySmall)
    Slider(value, onChange, valueRange = range)
}
