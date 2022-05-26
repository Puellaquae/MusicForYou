package puelloc.musicplayer.utils

class NFCUtils {
    companion object {
        val APDU_AID = byteArrayOf(0xF0.toByte(), 0x4D, 0x55, 0x53, 0x49, 0x43, 0x34, 0x55)

        fun selectApdu(aid: ByteArray): ByteArray {
            val commandApdu = ByteArray(6 + aid.size)
            commandApdu[0] = 0x00 // CLA
            commandApdu[1] = 0xA4.toByte() // INS
            commandApdu[2] = 0x04 // P1
            commandApdu[3] = 0x00 // P2
            commandApdu[4] = (aid.size and 0x0FF).toByte() // Lc
            aid.copyInto(commandApdu, 5)
            commandApdu[commandApdu.lastIndex] = 0x00 // Le
            return commandApdu
        }
    }
}