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

private const val MY_MEDIA_ROOT_ID = "media_root_id"

class MediaPlaybackService : MediaBrowserServiceCompat() {
    private lateinit var mediaSession: MediaSessionCompat
    private lateinit var mediaCallback: MediaCallback
    private lateinit var mediaNotificationManager: MediaNotificationManager

    companion object {
        private val TAG: String = MediaPlaybackService::class.java.simpleName
    }

    override fun onGetRoot(
        clientPackageName: String,
        clientUid: Int,
        rootHints: Bundle?
    ): BrowserRoot {
        return BrowserRoot(MY_MEDIA_ROOT_ID, null)
    }

    override fun onLoadChildren(
        parentId: String,
        result: Result<MutableList<MediaBrowserCompat.MediaItem>>
    ) {
        Log.d(TAG, "onLoadChildren")
        val mediaItems = listOf(MediaBrowserCompat.MediaItem(MediaMetadataCompat.Builder().apply {
            putString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID, "Song123")
            putString(MediaMetadataCompat.METADATA_KEY_TITLE, "Song")
        }
            .build().description, MediaBrowserCompat.MediaItem.FLAG_PLAYABLE))
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
                    setActions(PlaybackStateCompat.ACTION_PLAY)
                    setActions(PlaybackStateCompat.ACTION_SKIP_TO_NEXT)
                    setActions(PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS)
                }
                    .build()
            )
            isActive = true
        }
        sessionToken = mediaSession.sessionToken

        mediaNotificationManager = MediaNotificationManager(this, mediaSession.sessionToken)

        val notification = mediaNotificationManager.getNotification(false)
        startForeground(MediaNotificationManager.NOTIFICATION_ID, notification)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = MediaButtonReceiver.handleIntent(mediaSession, intent)
        Log.d(TAG, "keyEvent $action")
        return super.onStartCommand(intent, flags, startId)
    }

    fun play() {
        val notification = mediaNotificationManager.getNotification(true)
        startForeground(MediaNotificationManager.NOTIFICATION_ID, notification)
    }

    fun pause() {
        val notification = mediaNotificationManager.getNotification(false)
        startForeground(MediaNotificationManager.NOTIFICATION_ID, notification)
    }
}