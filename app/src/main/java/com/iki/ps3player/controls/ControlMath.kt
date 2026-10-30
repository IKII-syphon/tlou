package com.iki.ps3player.controls

import kotlin.math.min
import kotlin.math.sqrt

object ControlMath {
    /** Dead zone radial + rescale agar keluaran mulai dari 0 tepat di luar dead zone. */
    fun stick(x: Float, y: Float, deadZone: Float): Pair<Float, Float> {
        val dz = deadZone.coerceIn(0f, 0.9f)
        val mag = sqrt(x * x + y * y)
        if (mag == 0f || mag <= dz) return 0f to 0f
        val scaled = (min(mag, 1f) - dz) / (1f - dz)
        return (x / mag * scaled) to (y / mag * scaled)
    }
}
