package puelloc.musicplayer.observer

import android.content.Context
import android.database.ContentObserver
import android.os.Handler
import android.util.Log

class AudioObserver(private val context: Context, private val handler: Handler) :
    ContentObserver(handler) {

    override fun onChange(selfChange: Boolean) {
        super.onChange(selfChange)
        Log.d("AudioObserver", "onChange $selfChange")
        handler.obtainMessage()
    }
}