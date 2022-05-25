package puelloc.musicplayer.service

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.Drawable
import android.os.Build
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.core.graphics.drawable.IconCompat
import androidx.core.graphics.drawable.toBitmap
import androidx.core.text.parseAsHtml
import androidx.media.session.MediaButtonReceiver
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import puelloc.musicplayer.R
import puelloc.musicplayer.entity.Song
import puelloc.musicplayer.glide.audiocover.AudioCover
import puelloc.musicplayer.ui.activity.MainActivity
import puelloc.musicplayer.utils.VersionUtil

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
        R.drawable.ic_baseline_play_arrow_32,
        service.getString(R.string.play),
        MediaButtonReceiver.buildMediaButtonPendingIntent(
            service,
            PlaybackStateCompat.ACTION_PLAY
        )
    )
    private var pauseAction: NotificationCompat.Action = NotificationCompat.Action(
        R.drawable.ic_baseline_pause_32,
        service.getString(R.string.pause),
        MediaButtonReceiver.buildMediaButtonPendingIntent(
            service,
            PlaybackStateCompat.ACTION_PAUSE
        )
    )

    private var lastSong: Song? = null
    private val notificationBuilder = NotificationCompat.Builder(service, CHANNEL_ID).apply {
        setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
        setSmallIcon(R.drawable.ic_round_music_note_24)
        setContentIntent(
            PendingIntent.getActivity(
                service,
                0,
                Intent(service, MainActivity::class.java),
                PendingIntent.FLAG_CANCEL_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        )
        setShowWhen(false)
        addAction(prevAction)
        addAction(pauseAction)
        addAction(nextAction)
        setStyle(
            androidx.media.app.NotificationCompat.MediaStyle()
                .setShowActionsInCompactView(0, 1, 2)
                .setMediaSession(sessionToken)
        )
    }

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

    fun updateSong(song: Song) {
        notificationBuilder.setContentTitle(("<b>${song.name}</b>").parseAsHtml())
        notificationBuilder.setContentText(song.artistName)
        Glide.with(service.applicationContext).asBitmap().load(AudioCover(song.path))
            .into(object : CustomTarget<Bitmap>() {
                override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
                    notificationBuilder.setLargeIcon(resource)
                    notify(notificationBuilder.build())
                }

                override fun onLoadCleared(placeholder: Drawable?) {
                    notificationBuilder.setLargeIcon(
                        BitmapFactory.decodeResource(
                            service.resources,
                            R.drawable.default_audio_art
                        )
                    )
                    notify(notificationBuilder.build())
                }

                override fun onLoadFailed(errorDrawable: Drawable?) {
                    super.onLoadFailed(errorDrawable)
                    notificationBuilder.setLargeIcon(
                        BitmapFactory.decodeResource(
                            service.resources,
                            R.drawable.default_audio_art
                        )
                    )
                    notify(notificationBuilder.build())
                }
            })
    }

    @SuppressLint("RestrictedApi")
    fun updatePlay(isPlaying: Boolean) {
        notificationBuilder.mActions[1] = if (isPlaying) {
            pauseAction
        } else {
            playAction
        }
        notify(notificationBuilder.build())
    }

    private fun notify(
        notification: Notification
    ) {
        if (VersionUtil.O) {
            createChannel()
        }

        val update = notificationManager.activeNotifications.any { it.id == NOTIFICATION_ID }
        if (update) {
            notificationManager.notify(NOTIFICATION_ID, notification)
        } else {
            if (VersionUtil.Q) {
                service.startForeground(
                    NOTIFICATION_ID,
                    notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
                )
            } else {
                service.startForeground(NOTIFICATION_ID, notification)
            }
        }
    }

    fun stop() {
        service.stopForeground(true)
    }
}