package com.iki.ps3player.emulator

import com.iki.ps3player.controls.InputState
import com.iki.ps3player.controls.PadButton
import kotlin.math.roundToInt

/** Hasil pemetaan ke argumen overlayPadData(digital1, digital2, lx, ly, rx, ry). */
data class PadData(val d1: Int, val d2: Int, val lx: Int, val ly: Int, val rx: Int, val ry: Int)

/** Bit mengikuti Digital1Flags/Digital2Flags di rpcsx-ui-android; stik 0..255 dengan tengah 128. */
object RpcsxPadMapper {
    private val D1 = mapOf(
        PadButton.SELECT to 0x01, PadButton.START to 0x08,
        PadButton.UP to 0x10, PadButton.RIGHT to 0x20, PadButton.DOWN to 0x40, PadButton.LEFT to 0x80,
    )
    private val D2 = mapOf(
        PadButton.L2 to 0x01, PadButton.R2 to 0x02, PadButton.L1 to 0x04, PadButton.R1 to 0x08,
        PadButton.TRIANGLE to 0x10, PadButton.CIRCLE to 0x20, PadButton.CROSS to 0x40, PadButton.SQUARE to 0x80,
    )

    private fun axis(v: Float) = (v.coerceIn(-1f, 1f) * 127f + 128f).roundToInt().coerceIn(0, 255)

    fun map(s: InputState): PadData {
        var d1 = 0; var d2 = 0
        D1.forEach { (b, bit) -> if (s.isDown(b)) d1 = d1 or bit }
        D2.forEach { (b, bit) -> if (s.isDown(b)) d2 = d2 or bit }
        return PadData(d1, d2, axis(s.lx), axis(s.ly), axis(s.rx), axis(s.ry))
    }
}
