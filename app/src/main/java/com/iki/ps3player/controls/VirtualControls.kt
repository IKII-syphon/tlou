package com.iki.ps3player.controls

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.hypot
import kotlin.math.roundToInt

typealias InputSender = ((InputState) -> InputState) -> Unit

private class El(val id: String, val w: Dp, val h: Dp, val content: @Composable () -> Unit)

/**
 * Overlay kontrol multi-touch. Tiap elemen punya pointerInput sendiri, sehingga
 * jari berbeda dapat menekan stik dan tombol bersamaan.
 */
@Composable
fun VirtualControls(
    layout: ControlLayout,
    edit: Boolean,
    onMove: (id: String, dx: Float, dy: Float) -> Unit,
    onInput: InputSender,
) {
    val move by rememberUpdatedState(onMove)
    val send by rememberUpdatedState(onInput)
    val sens = layout.cameraSensitivity

    BoxWithConstraints(Modifier.fillMaxSize()) {
        val w = constraints.maxWidth.toFloat()
        val h = constraints.maxHeight.toFloat()
        val els = listOf(
            El("lstick", 120.dp, 120.dp) { AnalogStick(layout.deadZone) { x, y -> send { it.withLeft(x, y) } } },
            El("rstick", 120.dp, 120.dp) {
                AnalogStick(layout.deadZone) { x, y ->
                    send { it.withRight((x * sens).coerceIn(-1f, 1f), (y * sens).coerceIn(-1f, 1f)) }
                }
            },
            El("dpad", 130.dp, 130.dp) {
                Cluster(
                    listOf(
                        Triple("▲", Alignment.TopCenter, PadButton.UP),
                        Triple("▼", Alignment.BottomCenter, PadButton.DOWN),
                        Triple("◀", Alignment.CenterStart, PadButton.LEFT),
                        Triple("▶", Alignment.CenterEnd, PadButton.RIGHT),
                    ), send,
                )
            },
            El("face", 140.dp, 140.dp) {
                Cluster(
                    listOf(
                        Triple("△", Alignment.TopCenter, PadButton.TRIANGLE),
                        Triple("×", Alignment.BottomCenter, PadButton.CROSS),
                        Triple("□", Alignment.CenterStart, PadButton.SQUARE),
                        Triple("○", Alignment.CenterEnd, PadButton.CIRCLE),
                    ), send,
                )
            },
            wide("L1", PadButton.L1, send), wide("L2", PadButton.L2, send),
            wide("R1", PadButton.R1, send), wide("R2", PadButton.R2, send),
            wide("select", PadButton.SELECT, send, "SELECT"), wide("start", PadButton.START, send, "START"),
        )

        els.forEach { el ->
            val pos = layout.positions[el.id] ?: (0.5f to 0.5f)
            val wd = el.w * layout.scale
            val hd = el.h * layout.scale
            Box(
                Modifier
                    .size(wd, hd)
                    .offset { IntOffset((pos.first * w - wd.toPx() / 2).roundToInt(), (pos.second * h - hd.toPx() / 2).roundToInt()) }
                    .alpha(layout.alpha)
            ) {
                el.content()
                if (edit) {
                    Box(
                        Modifier.fillMaxSize().background(Color.Yellow.copy(0.3f))
                            .pointerInput(el.id, w, h) {
                                detectDragGestures { c, d -> c.consume(); move(el.id, d.x / w, d.y / h) }
                            }
                    )
                }
            }
        }
    }
}

private fun wide(id: String, b: PadButton, send: InputSender, label: String = id) =
    El(id, if (label.length > 3) 72.dp else 56.dp, 36.dp) {
        PadBtn(label, Modifier.fillMaxSize(), RoundedCornerShape(12.dp), 12.sp) { p -> send { it.withButton(b, p) } }
    }

@Composable
private fun Cluster(items: List<Triple<String, Alignment, PadButton>>, send: InputSender) {
    Box(Modifier.fillMaxSize()) {
        items.forEach { (label, align, btn) ->
            PadBtn(label, Modifier.align(align).fillMaxSize(0.36f)) { p -> send { it.withButton(btn, p) } }
        }
    }
}

@Composable
private fun PadBtn(
    label: String,
    modifier: Modifier,
    shape: Shape = CircleShape,
    fontSize: TextUnit = 18.sp,
    onPress: (Boolean) -> Unit,
) {
    var down by remember { mutableStateOf(false) }
    val cb by rememberUpdatedState(onPress)
    Box(
        modifier
            .background(if (down) Color.White.copy(0.6f) else Color.White.copy(0.25f), shape)
            .pointerInput(Unit) {
                awaitEachGesture {
                    awaitFirstDown()
                    down = true; cb(true)
                    waitForUpOrCancellation()
                    down = false; cb(false)
                }
            },
        contentAlignment = Alignment.Center,
    ) { Text(label, color = Color.White, fontSize = fontSize) }
}

@Composable
private fun AnalogStick(deadZone: Float, onChange: (Float, Float) -> Unit) {
    val cb by rememberUpdatedState(onChange)
    var knob by remember { mutableStateOf(Offset.Zero) }
    Box(
        Modifier.fillMaxSize()
            .drawBehind {
                drawCircle(Color.White.copy(0.2f))
                drawCircle(Color.White.copy(0.6f), size.minDimension * 0.22f, center + knob * (size.minDimension / 2f * 0.7f))
            }
            .pointerInput(deadZone) {
                val r = size.width / 2f
                awaitEachGesture {
                    val down = awaitFirstDown()
                    fun emit(p: Offset) {
                        var nx = (p.x - r) / r
                        var ny = (p.y - r) / r
                        val m = hypot(nx, ny)
                        if (m > 1f) { nx /= m; ny /= m }
                        knob = Offset(nx, ny)
                        val (x, y) = ControlMath.stick(nx, ny, deadZone)
                        cb(x, y)
                    }
                    emit(down.position)
                    while (true) {
                        val c = awaitPointerEvent().changes.firstOrNull { it.id == down.id } ?: break
                        if (!c.pressed) break
                        c.consume(); emit(c.position)
                    }
                    knob = Offset.Zero; cb(0f, 0f)
                }
            }
    )
}
