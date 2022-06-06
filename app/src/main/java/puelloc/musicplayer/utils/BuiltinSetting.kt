package puelloc.musicplayer.utils

import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaFeature
import android.media.MediaFormat
import java.util.*

class BuiltinSetting {
    enum class BluetoothProtocols {
        RFCOMM,
        // L2CAP,
        RFCOMM_INSECURE,
        // L2CAP_INSECURE
    }

    companion object {
        const val AUDIO_CAPTURE_SAMPLE_RATE = 44100

        val BUFFER_SIZE_IN_BYTES = AudioRecord.getMinBufferSize(AUDIO_CAPTURE_SAMPLE_RATE, 2, AudioFormat.ENCODING_PCM_16BIT)

        val BLUETOOTH_USE: BluetoothProtocols = BluetoothProtocols.RFCOMM_INSECURE

        val BLUETOOTH_RFCOMM_UUID: UUID = UUID.fromString("38400000-8cf0-11bd-b23e-10b96e4ef00e")

        const val PLAYLIST_RECURSIVE_BUILD_FROM_FOLDER = false
    }
}