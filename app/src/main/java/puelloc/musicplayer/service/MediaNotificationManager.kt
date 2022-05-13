package puelloc.musicplayer.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.media.session.MediaButtonReceiver
import puelloc.musicplayer.R

class MediaNotificationManager(
    private val service: MediaPlaybackService,
    private val sessionToken: MediaSessionCompat.Token
) {
    companion object {
        private const val CHANNEL_ID = "123"
        const val NOTIFICATION_ID = 146
    }

    private val notificationManager =
        service.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    private var prevAction: NotificationCompat.Action = NotificationCompat.Action(
        R.drawable.ic_baseline_skip_previous_24,
        service.getString(R.string.prev),
        MediaButtonReceiver.buildMediaButtonPendingIntent(
            service,
            PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS
        )
    )
    private var nextAction: NotificationCompat.Action = NotificationCompat.Action(
        R.drawable.ic_baseline_skip_next_24,
        service.getString(R.string.next),
        MediaButtonReceiver.buildMediaButtonPendingIntent(
            service,
            PlaybackStateCompat.ACTION_SKIP_TO_NEXT
        )
    )
    private var playAction: NotificationCompat.Action = NotificationCompat.Action(
        R.drawable.ic_baseline_play_arrow_24,
        service.getString(R.string.play),
        MediaButtonReceiver.buildMediaButtonPendingIntent(
            service,
            PlaybackStateCompat.ACTION_PLAY
        )
    )
    private var pauseAction: NotificationCompat.Action = NotificationCompat.Action(
        R.drawable.ic_baseline_pause_24,
        service.getString(R.string.pause),
        MediaButtonReceiver.buildMediaButtonPendingIntent(
            service,
            PlaybackStateCompat.ACTION_PAUSE
        )
    )

    @RequiresApi(Build.VERSION_CODES.O)
    private fun createChannel() {
        if (notificationManager.getNotificationChannel(CHANNEL_ID) == null) {
            val name = "Playback"
            val descriptionText = "Playback controller"
            val importance = NotificationManager.IMPORTANCE_LOW
            val mChannel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
                enableVibration(false)
            }
            notificationManager.createNotificationChannel(mChannel)
        }
    }

    private fun buildNotification(isPlaying: Boolean): NotificationCompat.Builder {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createChannel()
        }
        return NotificationCompat.Builder(service, CHANNEL_ID)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setSmallIcon(R.drawable.ic_round_music_note_24)
            .addAction(prevAction)
            .addAction(
                if (isPlaying) {
                    pauseAction
                } else {
                    playAction
                }
            )
            .addAction(nextAction)
            .setStyle(
                androidx.media.app.NotificationCompat.MediaStyle()
                    .setShowActionsInCompactView(0, 1, 2)
                    .setMediaSession(sessionToken)
            )
            .setContentTitle("Wonderful music")
            .setContentText("My Awesome Band")
    }

    fun getNotification(isPlaying: Boolean): Notification {
        return buildNotification(isPlaying).build()
    }
}