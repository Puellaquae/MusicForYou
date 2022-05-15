package puelloc.musicplayer.service

import android.app.PendingIntent
import android.app.PendingIntent.FLAG_IMMUTABLE
import android.content.Intent
import android.os.Bundle
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.util.Log
import androidx.media.MediaBrowserServiceCompat
import androidx.media.session.MediaButtonReceiver
import puelloc.musicplayer.ui.activity.MainActivity
import puelloc.musicplayer.viewmodel.service.MediaPlayState

class MediaPlaybackService : MediaBrowserServiceCompat() {
    private lateinit var mediaSession: MediaSessionCompat
    private lateinit var mediaCallback: MediaCallback
    private lateinit var mediaNotificationManager: MediaNotificationManager
    private val mediaPlayState = MediaPlayState.getInstance()

    companion object {
        private val TAG: String = MediaPlaybackService::class.java.simpleName
        private const val MY_MEDIA_ROOT_ID = "media_root_id"
    }

    override fun onGetRoot(
        clientPackageName: String,
        clientUid: Int,
        rootHints: Bundle?
    ): BrowserRoot {
        Log.d(TAG, "getRoot from $clientPackageName")
        return BrowserRoot(MY_MEDIA_ROOT_ID, null)
    }

    override fun onLoadChildren(
        parentId: String,
        result: Result<MutableList<MediaBrowserCompat.MediaItem>>
    ) {
        Log.d(TAG, "loadChildren from $parentId")
        val mediaItems = emptyList<MediaBrowserCompat.MediaItem>()
        result.sendResult(mediaItems.toMutableList())
    }

    override fun onCreate() {
        super.onCreate()

        val sessionActivityPendingIntent =
            packageManager?.getLaunchIntentForPackage(packageName)?.let {
                PendingIntent.getActivity(this@MediaPlaybackService, 0, it, FLAG_IMMUTABLE)
            }

        mediaCallback = MediaCallback(this)

        mediaSession = MediaSessionCompat(this, "MusicService").apply {
            setSessionActivity(sessionActivityPendingIntent)
            setCallback(mediaCallback)
            setPlaybackState(
                PlaybackStateCompat.Builder().apply {
                    setActions(
                        PlaybackStateCompat.ACTION_PLAY or
                                PlaybackStateCompat.ACTION_PAUSE or
                                PlaybackStateCompat.ACTION_PLAY_PAUSE or
                                PlaybackStateCompat.ACTION_STOP or
                                PlaybackStateCompat.ACTION_SKIP_TO_NEXT or
                                PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS
                    )
                    setState(
                        PlaybackStateCompat.STATE_STOPPED,
                        PlaybackStateCompat.PLAYBACK_POSITION_UNKNOWN,
                        1F
                    )
                }.build()
            )
            isActive = true
        }
        sessionToken = mediaSession.sessionToken

        mediaNotificationManager = MediaNotificationManager(this, mediaSession.sessionToken)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = MediaButtonReceiver.handleIntent(mediaSession, intent)
        Log.d(TAG, "keyEvent $action")
        return super.onStartCommand(intent, flags, startId)
    }

    fun play() {
        mediaPlayState.song?.let {
            mediaSession.setPlaybackState(PlaybackStateCompat.Builder().apply {
                setActions(
                    PlaybackStateCompat.ACTION_PLAY or
                            PlaybackStateCompat.ACTION_PAUSE or
                            PlaybackStateCompat.ACTION_PLAY_PAUSE or
                            PlaybackStateCompat.ACTION_STOP or
                            PlaybackStateCompat.ACTION_SKIP_TO_NEXT or
                            PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS
                )
                setState(
                    PlaybackStateCompat.STATE_PLAYING,
                    PlaybackStateCompat.PLAYBACK_POSITION_UNKNOWN,
                    1F
                )
            }.build())
            mediaNotificationManager.showNotification(it, true)
        }
    }

    fun pause() {
        mediaPlayState.song?.let {
            mediaNotificationManager.showNotification(it, false)
        }
    }
}