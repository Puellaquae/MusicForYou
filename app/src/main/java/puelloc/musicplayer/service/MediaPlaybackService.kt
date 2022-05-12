package puelloc.musicplayer.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.PendingIntent.FLAG_IMMUTABLE
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.media.MediaBrowserServiceCompat
import androidx.media.session.MediaButtonReceiver
import puelloc.musicplayer.R
import puelloc.musicplayer.ui.activity.MainActivity

private const val MY_MEDIA_ROOT_ID = "media_root_id"
private const val MY_EMPTY_MEDIA_ROOT_ID = "empty_root_id"
private const val CHANNEL_ID = "123"

class MediaPlaybackService : MediaBrowserServiceCompat() {
    private lateinit var mediaSession: MediaSessionCompat
    private lateinit var stateBuilder: PlaybackStateCompat.Builder

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
        if (MY_EMPTY_MEDIA_ROOT_ID == parentId) {
            result.sendResult(null)
            return
        }

        // Assume for example that the music catalog is already loaded/cached.

        val mediaItems = emptyList<MediaBrowserCompat.MediaItem>()

        // Check if this is the root menu:
        if (MY_MEDIA_ROOT_ID == parentId) {
            // Build the MediaItem objects for the top level,
            // and put them in the mediaItems list...
        } else {
            // Examine the passed parentMediaId to see which submenu we're at,
            // and put the children of that menu in the mediaItems list...
        }
        result.sendResult(mediaItems.toMutableList())
    }

    override fun onCreate() {
        super.onCreate()

        Log.d("MediaPlaybackService@onCreate", "creating")

        mediaSession = MediaSessionCompat(this, "MusicService").apply {
            setCallback(object : MediaSessionCompat.Callback() {
                override fun onPlay() {
                    super.onPlay()
                    Log.d("MediaSessionCompat@Callback", "Play")
                }

                override fun onPause() {
                    super.onPause()
                    Log.d("MediaSessionCompat@Callback", "Pause")
                }

                override fun onStop() {
                    super.onStop()
                    Log.d("MediaSessionCompat@Callback", "Stop")
                }
            })

            setPlaybackState(
                PlaybackStateCompat.Builder().setState(
                    PlaybackStateCompat.STATE_PLAYING,
                    PlaybackStateCompat.PLAYBACK_POSITION_UNKNOWN,
                    0F
                ).build()
            )

            isActive = true
        }

        Log.d("MediaPlaybackService@onCreate", "mediaSession created")

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Create the NotificationChannel
            val name = "Playback"
            val descriptionText = "Playback controller"
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val mChannel = NotificationChannel(CHANNEL_ID, name, importance)
            mChannel.description = descriptionText
            // Register the channel with the system; you can't change the importance
            // or other notification behaviors after this
            val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(mChannel)
        }

        Log.d("MediaPlaybackService@onCreate", "created notification channel")

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            // Show controls on lock screen even when user hides sensitive content.
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setSmallIcon(R.drawable.ic_round_music_note_24)
            // Add media control buttons that invoke intents in your media service
            .addAction(
                NotificationCompat.Action(
                    R.drawable.ic_baseline_skip_previous_24,
                    "Prev",
                    MediaButtonReceiver.buildMediaButtonPendingIntent(
                        this,
                        PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS
                    )
                )
            ) // #0
            .addAction(
                NotificationCompat.Action(
                    R.drawable.ic_baseline_pause_24,
                    getString(R.string.pause),
                    MediaButtonReceiver.buildMediaButtonPendingIntent(
                        this,
                        PlaybackStateCompat.ACTION_PLAY_PAUSE
                    )
                )
            ) // #1
            .addAction(
                NotificationCompat.Action(
                    R.drawable.ic_baseline_skip_next_24,
                    "Next",
                    MediaButtonReceiver.buildMediaButtonPendingIntent(
                        this,
                        PlaybackStateCompat.ACTION_SKIP_TO_NEXT
                    )
                )
            ) // #2
            // Apply the media style template
            .setStyle(
                androidx.media.app.NotificationCompat.MediaStyle()
                    .setShowActionsInCompactView(0, 1, 2 /* #1: pause button \*/)
                    .setMediaSession(mediaSession.sessionToken)
            )
            .setContentTitle("Wonderful music")
            .setContentText("My Awesome Band")
            .build()

        startForeground(146, notification)

        Log.d("MediaPlaybackService@onCreate", "foreground started")
    }
}