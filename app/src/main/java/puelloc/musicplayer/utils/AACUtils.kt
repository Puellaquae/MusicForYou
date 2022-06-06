package puelloc.musicplayer.utils

class AACUtils {
    companion object {
        fun addADTS(frame: ByteArray) {
            val profile = 2 // ACC_LC
            val freqIdx = 4 // 44.1 kHz
            val channelCnt = 2

            val size = frame.size

            frame[0] = 0xFF.toByte()
            frame[1] = 0xF9.toByte()
            frame[2] = (((profile - 1) shl 6) + (freqIdx shl 2) + (channelCnt shr 2)).toByte()
            frame[3] = (((channelCnt and 3) shl 6) + (size shr 11)).toByte()
            frame[4] = ((size and 0x7FF) shr 3).toByte()
            frame[5] = (((size and 7) shl 5) + 0x1F).toByte()
            frame[6] = 0xFC.toByte()
        }

        fun frameSize(frame: ByteArray, offset: Int = 0): Int {
            if (frame.size - offset < 7) {
                return -1
            }
            if (frame[0 + offset] != 0xFF.toByte() && frame[1 + offset] != 0xF9.toByte() && frame[6 + offset] != 0xFC.toByte()) {
                return -1
            }

            val fBit = frame[3 + offset].toInt() and 0xFF
            val mBit = frame[4 + offset].toInt() and 0xFF
            val bBit = frame[5 + offset].toInt() and 0xFF
            return (bBit shr 5) + (mBit shl 3) + ((fBit and 3) shr 11)
        }
    }
}