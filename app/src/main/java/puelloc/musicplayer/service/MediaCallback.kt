package puelloc.musicplayer.service

import android.content.Intent
import android.support.v4.media.session.MediaSessionCompat
import android.util.Log

class MediaCallback(private val mediaPlaybackService: MediaPlaybackService) : MediaSessionCompat.Callback() {
    companion object {
        private val TAG = MediaCallback::class.java.simpleName
    }

    override fun onPlay() {
        super.onPlay()
        mediaPlaybackService.play()
        Log.d(TAG, "Play")
    }

    override fun onPause() {
        super.onPause()
        Log.d(TAG, "Pause")
    }

    override fun onStop() {
        super.onStop()
        Log.d(TAG, "Stop")
    }

    override fun onSkipToNext() {
        super.onSkipToNext()
        Log.d(TAG, "SkipToNext")
    }

    override fun onSkipToPrevious() {
        super.onSkipToPrevious()
        Log.d(TAG, "SkipToPrevious")
    }

    override fun onMediaButtonEvent(mediaButtonEvent: Intent?): Boolean {
        Log.d(TAG, "mediaEvent ${mediaButtonEvent?.action}")
        return super.onMediaButtonEvent(mediaButtonEvent)
    }
}