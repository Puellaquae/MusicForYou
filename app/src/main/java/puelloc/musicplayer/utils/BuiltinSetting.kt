package puelloc.musicplayer.utils

import android.media.AudioFormat
import java.util.*

class BuiltinSetting {
    enum class BluetoothProtocols {
        RFCOMM,
        // L2CAP,
        RFCOMM_INSECURE,
        // L2CAP_INSECURE
    }

    companion object {
        const val USED_AUDIO_ENCODE = AudioFormat.ENCODING_PCM_16BIT
        const val USED_SAMPLE_RATE = 8000

        const val BUFFER_SIZE_IN_BYTES = 1024

        val BLUETOOTH_USE: BluetoothProtocols = BluetoothProtocols.RFCOMM_INSECURE

        val BLUETOOTH_RFCOMM_UUID: UUID = UUID.fromString("38400000-8cf0-11bd-b23e-10b96e4ef00e")

        val PLAYLIST_RECURSIVE_BUILD_FROM_FLODER = false
    }
}