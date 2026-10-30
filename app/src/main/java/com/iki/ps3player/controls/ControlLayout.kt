package com.iki.ps3player.controls

/** Posisi elemen = pusat elemen sebagai pecahan (0..1) dari lebar/tinggi layar. */
data class ControlLayout(
    val alpha: Float = 0.5f,
    val scale: Float = 1f,
    val deadZone: Float = 0.15f,
    val cameraSensitivity: Float = 1f,
    val positions: Map<String, Pair<Float, Float>> = DEFAULT_POSITIONS,
) {
    fun moved(id: String, dx: Float, dy: Float): ControlLayout {
        val p = positions[id] ?: return this
        val np = (p.first + dx).coerceIn(0f, 1f) to (p.second + dy).coerceIn(0f, 1f)
        return copy(positions = positions + (id to np))
    }

    /** Preset rasio layar: 20:9 ke atas merapatkan kontrol ke tepi. */
    fun withPreset(aspect: Float): ControlLayout {
        if (aspect < 2f) return copy(positions = DEFAULT_POSITIONS)
        val squeezed = DEFAULT_POSITIONS.mapValues { (id, p) ->
            if (id == "select" || id == "start") p
            else (if (p.first < 0.5f) p.first * 0.8f else 1f - (1f - p.first) * 0.8f) to p.second
        }
        return copy(positions = squeezed)
    }

    fun encode(): String =
        listOf(alpha, scale, deadZone, cameraSensitivity).joinToString(",") + "|" +
            positions.entries.joinToString(";") { "${it.key}:${it.value.first}:${it.value.second}" }

    companion object {
        val DEFAULT_POSITIONS: Map<String, Pair<Float, Float>> = mapOf(
            "lstick" to (0.17f to 0.72f), "dpad" to (0.15f to 0.36f),
            "face" to (0.85f to 0.36f), "rstick" to (0.80f to 0.74f),
            "L1" to (0.06f to 0.12f), "L2" to (0.16f to 0.12f),
            "R1" to (0.94f to 0.12f), "R2" to (0.84f to 0.12f),
            "select" to (0.42f to 0.92f), "start" to (0.58f to 0.92f),
        )

        fun decode(s: String?): ControlLayout {
            if (s == null) return ControlLayout()
            return runCatching {
                val (head, pos) = s.split("|", limit = 2)
                val h = head.split(",").map { it.toFloat() }
                val p = pos.split(";").filter { it.isNotBlank() }.associate {
                    val t = it.split(":")
                    t[0] to (t[1].toFloat() to t[2].toFloat())
                }
                ControlLayout(h[0], h[1], h[2], h[3], DEFAULT_POSITIONS + p)
            }.getOrDefault(ControlLayout())
        }
    }
}
