package puelloc.musicplayer.service

import android.content.Intent
import android.support.v4.media.session.MediaSessionCompat
import android.util.Log
import puelloc.musicplayer.enums.PlaybackEvent

class MediaCallback(private val mediaPlaybackService: MediaPlaybackService) : MediaSessionCompat.Callback() {
    companion object {
        private val TAG = this::class.java.declaringClass.simpleName
    }

    override fun onPlay() {
        super.onPlay()
        mediaPlaybackService.emit(PlaybackEvent.PLAY)
        Log.d(TAG, "Play")
    }

    override fun onPause() {
        super.onPause()
        mediaPlaybackService.emit(PlaybackEvent.PAUSE)
        Log.d(TAG, "Pause")
    }

    override fun onStop() {
        super.onStop()
        mediaPlaybackService.emit(PlaybackEvent.STOP)
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