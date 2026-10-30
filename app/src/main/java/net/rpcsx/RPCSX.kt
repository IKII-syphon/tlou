package net.rpcsx

import android.view.Surface

/**
 * Subset binding JNI dari fork rpcsx-ui-android (GPLv2). Nama paket dan kelas harus tetap
 * net.rpcsx.RPCSX karena simbol native bernama Java_net_rpcsx_RPCSX_*.
 * Membutuhkan libnative-glue "rpcsx-android" (lihat README, langkah native glue).
 */
class RPCSX {
    external fun openLibrary(path: String): Boolean
    external fun initialize(rootDir: String, user: String): Boolean
    external fun startMainThreadProcessor(): Boolean
    external fun processCompilationQueue(): Boolean
    external fun installFw(fd: Int, progressId: Long): Boolean
    external fun install(fd: Int, progressId: Long): Boolean
    external fun collectGameInfo(rootDir: String, progressId: Long): Boolean
    external fun boot(path: String): Int
    external fun getState(): Int
    external fun kill()
    external fun resume()
    external fun openHomeMenu()
    external fun surfaceEvent(surface: Surface, event: Int): Boolean
    external fun overlayPadData(digital1: Int, digital2: Int, leftStickX: Int, leftStickY: Int, rightStickX: Int, rightStickY: Int): Boolean
    external fun getFrameWorkNanos(): Long
    external fun getFramePeriodNanos(): Long

    companion object {
        val instance: RPCSX by lazy { RPCSX() }
        init { System.loadLibrary("rpcsx-android") }
    }
}
