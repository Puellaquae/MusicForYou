package puelloc.musicplayer.observer

import android.content.Context
import android.database.ContentObserver
import android.os.Handler
import android.provider.MediaStore

class AudioObserver(handler: Handler) : ContentObserver(handler) {
    fun register(context: Context) {
        context.contentResolver.registerContentObserver(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, true, this)
    }


}