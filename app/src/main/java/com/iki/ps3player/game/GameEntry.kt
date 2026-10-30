package com.iki.ps3player.game

enum class CompatStatus(val label: String) {
    NOT_TESTED("Belum diuji"),
    CAN_OPEN("Bisa membuka game"),
    PAST_MENU("Bisa melewati menu"),
    IN_GAMEPLAY("Bisa masuk gameplay"),
    PLAYABLE_SCENE("Bisa bermain pada adegan tertentu"),
    STABLE_LONG_TERM("Stabil dalam pengujian jangka panjang"),
}

data class GameEntry(
    val id: String,
    val name: String,
    val uri: String,
    val sizeBytes: Long,
    val status: CompatStatus = CompatStatus.NOT_TESTED,
)

object GameValidator {
    val SUPPORTED = setOf("iso", "pkg")

    fun validate(fileName: String, sizeBytes: Long): Result<Unit> {
        val ext = fileName.substringAfterLast('.', "").lowercase()
        return when {
            ext !in SUPPORTED -> Result.failure(IllegalArgumentException(
                "Format \".$ext\" tidak didukung. Format yang diterima: ${SUPPORTED.joinToString { ".$it" }}"))
            sizeBytes <= 0 -> Result.failure(IllegalArgumentException("File kosong atau ukuran tidak terbaca."))
            else -> Result.success(Unit)
        }
    }
}
