package puelloc.musicplayer.service

import android.content.Intent
import android.support.v4.media.session.MediaSessionCompat
import android.util.Log

class MediaCallback(private val mediaPlaybackService: MediaPlaybackService) : MediaSessionCompat.Callback() {
    companion object {
        private val TAG = this::class.java.declaringClass.simpleName
    }

    override fun onPlay() {
        super.onPlay()
        mediaPlaybackService.play()
        Log.d(TAG, "Play")
    }

    override fun onPause() {
        super.onPause()
        mediaPlaybackService.pause()
        Log.d(TAG, "Pause")
    }

    override fun onStop() {
        super.onStop()
        mediaPlaybackService.stop()
        Log.d(TAG, "Stop")
    }

    override fun onSkipToNext() {
        super.onSkipToNext()
        mediaPlaybackService.skipToNext()
        Log.d(TAG, "SkipToNext")
    }

    override fun onSkipToPrevious() {
        super.onSkipToPrevious()
        mediaPlaybackService.skipToPrevious()
        Log.d(TAG, "SkipToPrevious")
    }

    override fun onSeekTo(pos: Long) {
        super.onSeekTo(pos)
        mediaPlaybackService.seekTo(pos)
        Log.d(TAG, "SeekTo $pos")
    }

    override fun onMediaButtonEvent(mediaButtonEvent: Intent?): Boolean {
        Log.d(TAG, "mediaEvent ${mediaButtonEvent?.action}")
        return super.onMediaButtonEvent(mediaButtonEvent)
    }
}