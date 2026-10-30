package com.iki.ps3player.emulator

import android.net.Uri

data class InstalledGame(val path: String, val name: String?)

/** Dimiliki backend yang bisa memasang firmware/game ke penyimpanan core. */
interface CoreInstaller {
    suspend fun installFirmware(uri: Uri): Result<Unit>
    suspend fun installGame(uri: Uri): Result<Unit>
    suspend fun scanGames(): List<InstalledGame>
}
