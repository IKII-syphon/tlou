package com.iki.ps3player

import com.iki.ps3player.controls.InputState
import com.iki.ps3player.controls.PadButton
import com.iki.ps3player.emulator.RpcsxPadMapper
import org.junit.Assert.assertEquals
import org.junit.Test

class RpcsxPadMapperTest {
    @Test fun neutralIsCentered() {
        assertEquals(com.iki.ps3player.emulator.PadData(0, 0, 128, 128, 128, 128), RpcsxPadMapper.map(InputState()))
    }
    @Test fun faceAndShoulderBits() {
        val s = InputState().withButton(PadButton.CROSS, true).withButton(PadButton.L1, true).withButton(PadButton.START, true)
        val p = RpcsxPadMapper.map(s)
        assertEquals(0x40 or 0x04, p.d2); assertEquals(0x08, p.d1)
    }
    @Test fun dpadBits() {
        val p = RpcsxPadMapper.map(InputState().withButton(PadButton.UP, true).withButton(PadButton.LEFT, true))
        assertEquals(0x10 or 0x80, p.d1)
    }
    @Test fun sticksClampedTo0to255() {
        val p = RpcsxPadMapper.map(InputState(lx = -5f, ly = 5f, rx = 1f, ry = -1f))
        assertEquals(1, p.lx); assertEquals(255, p.ly); assertEquals(255, p.rx); assertEquals(1, p.ry)
    }
}
