package com.iki.ps3player

import com.iki.ps3player.controls.*
import kotlin.math.hypot
import org.junit.Assert.*
import org.junit.Test

class ControlsTest {
    @Test fun insideDeadZone_isZero() {
        val (x, y) = ControlMath.stick(0.1f, 0.05f, 0.15f)
        assertEquals(0f, x, 0f); assertEquals(0f, y, 0f)
    }
    @Test fun fullDeflection_isUnit() {
        val (x, y) = ControlMath.stick(1f, 0f, 0.15f)
        assertEquals(1f, x, 1e-4f); assertEquals(0f, y, 1e-4f)
    }
    @Test fun overshoot_isClamped() {
        val (x, y) = ControlMath.stick(3f, 4f, 0.1f)
        assertEquals(1f, hypot(x, y), 1e-4f)
    }
    @Test fun directionPreserved() {
        val (x, y) = ControlMath.stick(-0.5f, 0.5f, 0.1f)
        assertTrue(x < 0 && y > 0); assertEquals(-x, y, 1e-4f)
    }
    @Test fun buttonsAreIndependent() {
        var s = InputState().withButton(PadButton.CROSS, true).withButton(PadButton.L1, true)
        assertTrue(s.isDown(PadButton.CROSS) && s.isDown(PadButton.L1))
        s = s.withButton(PadButton.CROSS, false)
        assertFalse(s.isDown(PadButton.CROSS)); assertTrue(s.isDown(PadButton.L1))
    }
    @Test fun allButtonBitsUnique() {
        assertEquals(PadButton.values().size, PadButton.values().map { it.bit }.toSet().size)
    }
    @Test fun layoutRoundTrip() {
        val l = ControlLayout(alpha = 0.3f, scale = 1.2f, deadZone = 0.2f, cameraSensitivity = 1.5f).moved("lstick", 0.05f, -0.1f)
        assertEquals(l, ControlLayout.decode(l.encode()))
    }
    @Test fun decodeGarbage_givesDefault() {
        assertEquals(ControlLayout(), ControlLayout.decode("xyz"))
        assertEquals(ControlLayout(), ControlLayout.decode(null))
    }
    @Test fun moveIsClampedToScreen() {
        assertEquals(1f to 0f, ControlLayout().moved("lstick", 5f, -5f).positions["lstick"])
    }
    @Test fun presets() {
        val d = ControlLayout.DEFAULT_POSITIONS
        assertEquals(d, ControlLayout().withPreset(16f / 9f).positions)
        assertTrue(ControlLayout().withPreset(20f / 9f).positions["lstick"]!!.first < d["lstick"]!!.first)
    }
}
