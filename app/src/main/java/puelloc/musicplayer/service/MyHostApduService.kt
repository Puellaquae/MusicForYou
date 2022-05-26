package puelloc.musicplayer.service

import android.nfc.cardemulation.HostApduService
import android.os.Bundle
import android.util.Log

class MyHostApduService : HostApduService() {
    override fun processCommandApdu(commandApdu: ByteArray?, extras: Bundle?): ByteArray {
        Log.d("MyHostApduService", "processCommandApdu: $commandApdu")

        return "MUSIC4U: Hello!".toByteArray(Charsets.UTF_8)
    }

    override fun onDeactivated(reason: Int) {
        Log.d("MyHostApduService", "onDeactivated: $reason")
    }
}