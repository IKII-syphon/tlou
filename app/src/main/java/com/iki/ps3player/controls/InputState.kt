package com.iki.ps3player.controls

enum class PadButton(val bit: Int) {
    CROSS(1), CIRCLE(2), SQUARE(4), TRIANGLE(8),
    L1(16), R1(32), L2(64), R2(128),
    START(256), SELECT(512),
    UP(1024), DOWN(2048), LEFT(4096), RIGHT(8192)
}

/** Status controller virtual. Y stik mengikuti koordinat layar (atas = negatif). */
data class InputState(
    val buttons: Int = 0,
    val lx: Float = 0f, val ly: Float = 0f,
    val rx: Float = 0f, val ry: Float = 0f,
) {
    fun isDown(b: PadButton) = (buttons and b.bit) != 0
    fun withButton(b: PadButton, pressed: Boolean) =
        copy(buttons = if (pressed) buttons or b.bit else buttons and b.bit.inv())
    fun withLeft(x: Float, y: Float) = copy(lx = x, ly = y)
    fun withRight(x: Float, y: Float) = copy(rx = x, ry = y)
}
