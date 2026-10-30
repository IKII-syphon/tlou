package com.iki.ps3player.emulator

import android.net.Uri

/** Satu-satunya pintu UI ke backend, sehingga backend dapat diganti tanpa menyentuh UI. */
class EmulatorManager(val backend: EmulatorBackend) {
    suspend fun prepare(uri: Uri): Result<Unit> {
        backend.initialize().onFailure { return Result.failure(it) }
        return backend.loadGame(uri)
    }
}
