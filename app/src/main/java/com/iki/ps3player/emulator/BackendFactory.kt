package com.iki.ps3player.emulator

import android.content.Context
import java.io.File

object BackendFactory {
    /** Core diletakkan di <files>/core/librpcsx-android.so (unduh sendiri dari rilis RPCSX-Clanker). */
    fun corePath(ctx: Context) = File(ctx.filesDir, "core/librpcsx-android.so")

    fun create(ctx: Context): EmulatorBackend {
        val core = corePath(ctx)
        return if (RpcsxBackend.isGlueAvailable() && core.exists()) RpcsxBackend(ctx, core.absolutePath) else MockBackend()
    }
}
