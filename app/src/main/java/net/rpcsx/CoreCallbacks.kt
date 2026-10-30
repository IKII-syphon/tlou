package net.rpcsx

import androidx.annotation.Keep
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * Kelas-kelas ini dicari oleh core (librpcsx-android.so) lewat nama: net/rpcsx/GameInfo,
 * GameRepository.add([GameInfo;J)V, ProgressRepository.onProgressEvent(JJJLString;)Z,
 * FirmwareRepository.onFirmwareInstalled(String)V. Jangan ubah nama/signature.
 */
data class GameInfo @Keep @JvmOverloads constructor(
    val path: String,
    var name: String? = null,
    var iconPath: String? = null,
    var gameFlags: Int = 0,
    var version: String? = null,
    var titleId: String? = null,
)

object CoreEvents {
    val games = MutableStateFlow<List<GameInfo>>(emptyList())
    val firmware = MutableStateFlow<String?>(null)
    /** value, max; value < 0 berarti gagal; max == 0 berarti tidak tentu. */
    val progress = MutableStateFlow<Pair<Long, Long>?>(null)
}

class GameRepository {
    companion object {
        @Keep @JvmStatic
        fun add(gameInfos: Array<GameInfo>, progressId: Long) {
            synchronized(CoreEvents) { CoreEvents.games.value = (CoreEvents.games.value + gameInfos).distinctBy { it.path } }
        }
    }
}

class ProgressRepository {
    companion object {
        @Keep @JvmStatic
        fun onProgressEvent(id: Long, value: Long, max: Long, message: String?): Boolean {
            CoreEvents.progress.value = value to max
            return true
        }
    }
}

class FirmwareRepository {
    companion object {
        @Keep @JvmStatic
        fun onFirmwareInstalled(version: String?) { CoreEvents.firmware.value = version ?: "terpasang" }
        @Keep @JvmStatic
        fun onFirmwareCompiled(version: String?) { CoreEvents.firmware.value = version ?: "terpasang" }
    }
}
