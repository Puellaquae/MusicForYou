package puelloc.musicplayer.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Bundle
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.util.Log
import androidx.lifecycle.Observer
import androidx.media.MediaBrowserServiceCompat
import androidx.media.session.MediaButtonReceiver
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import puelloc.musicplayer.entity.Song
import puelloc.musicplayer.glide.audiocover.AudioCover
import puelloc.musicplayer.utils.SongUtil.Companion.getMetadataBuilder
import puelloc.musicplayer.viewmodel.PlaybackQueueViewModel

class MediaPlaybackService : MediaBrowserServiceCompat() {
    private lateinit var mediaSession: MediaSessionCompat
    private lateinit var mediaCallback: MediaCallback
    private lateinit var mediaNotificationManager: MediaNotificationManager
    private lateinit var playbackQueueViewModel: PlaybackQueueViewModel
    private lateinit var mediaPlayer: MediaPlayer
    private val intentFilter = IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY)
    private val noisyAudioStreamReceiver = BecomingNoisyReceiver()
    private val currentSongObserver = Observer<Song?> {
        Log.d(TAG, "${it?.name} playing: ${playbackQueueViewModel.playing.value}")
        if (it != null) {
            if (it == prepareSong) {
                if (mediaPlayer.isPlaying) {
                    playbackQueueViewModel.playing.postValue(false)
                    pause()
                } else {
                    playbackQueueViewModel.playing.postValue(true)
                    play()
                }
            } else {
                prepare()
                if (playbackQueueViewModel.playing.value == true) {
                    play()
                }
            }
        }
    }

    companion object {
        private val TAG = this::class.java.declaringClass.simpleName
        private const val MY_MEDIA_ROOT_ID = "media_root_id"
        private const val PLAYBACK_ACTION = PlaybackStateCompat.ACTION_PLAY or
                PlaybackStateCompat.ACTION_PAUSE or
                PlaybackStateCompat.ACTION_PLAY_PAUSE or
                PlaybackStateCompat.ACTION_STOP or
                PlaybackStateCompat.ACTION_SKIP_TO_NEXT or
                PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS or
                PlaybackStateCompat.ACTION_SEEK_TO
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
        Log.d(TAG, "onCreate")

        playbackQueueViewModel = PlaybackQueueViewModel.getInstance(this.application)
        mediaCallback = MediaCallback(this)

        mediaSession = MediaSessionCompat(this, "MusicService").apply {
            setCallback(mediaCallback)
            setPlaybackState(
                PlaybackStateCompat.Builder().apply {
                    setActions(PLAYBACK_ACTION)
                    setState(
                        PlaybackStateCompat.STATE_STOPPED,
                        PlaybackStateCompat.PLAYBACK_POSITION_UNKNOWN,
                        1F
                    )
                }.build()
            )
            setSessionToken(sessionToken)
        }

        mediaNotificationManager = MediaNotificationManager(this, mediaSession.sessionToken)
        mediaPlayer = MediaPlayer()
        mediaPlayer.setOnCompletionListener {
            skipToNext()
        }
        playbackQueueViewModel.currentSong.observeForever(currentSongObserver)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = MediaButtonReceiver.handleIntent(mediaSession, intent)
        Log.d(TAG, "keyEvent $action")
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        Log.d(TAG, "onDestroy")
        super.onDestroy()
        mediaPlayer.release()
        playbackQueueViewModel.currentSong.removeObserver(currentSongObserver)
    }

    private var prepareSong: Song? = null

    private fun prepare() {
        prepareSong = playbackQueueViewModel.currentSong.value
        prepareSong?.let {
            mediaPlayer.reset()
            mediaPlayer.setDataSource(it.path)
            mediaPlayer.prepare()
            mediaSession.setPlaybackState(PlaybackStateCompat.Builder().apply {
                setActions(PLAYBACK_ACTION)
                setState(
                    PlaybackStateCompat.STATE_STOPPED,
                    0L,
                    1F
                )
            }.build())
            val metadata = it.getMetadataBuilder()
            Glide.with(this.applicationContext)
                .asBitmap()
                .load(AudioCover(it.path))
                .into(object : CustomTarget<Bitmap>() {
                    override fun onResourceReady(
                        resource: Bitmap,
                        transition: Transition<in Bitmap>?
                    ) {
                        metadata.putBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART, resource)
                        mediaSession.setMetadata(metadata.build())
                    }

                    override fun onLoadCleared(placeholder: Drawable?) {
                        mediaSession.setMetadata(metadata.build())
                    }

                    override fun onLoadFailed(errorDrawable: Drawable?) {
                        mediaSession.setMetadata(metadata.build())
                    }
                })
            mediaNotificationManager.updateSong(it)
        }
    }

    fun play(nextSongIfNeed: Boolean = false) {
        if (prepareSong == null) {
            prepare()
        }
        if (prepareSong == null && nextSongIfNeed) {
            playbackQueueViewModel.playing.postValue(true)
            skipToNext()
        }
        prepareSong?.let {
            registerReceiver(noisyAudioStreamReceiver, intentFilter)
            playbackQueueViewModel.playing.postValue(true)
            mediaPlayer.start()
            mediaSession.setPlaybackState(PlaybackStateCompat.Builder().apply {
                setActions(PLAYBACK_ACTION)
                setState(
                    PlaybackStateCompat.STATE_PLAYING,
                    mediaPlayer.currentPosition.toLong(),
                    1F
                )
            }.build())
            mediaNotificationManager.updatePlay(true)
        }
    }

    fun stop() {
        prepareSong?.let { mediaNotificationManager.updatePlay(false) }
        prepareSong = null
        playbackQueueViewModel.playing.postValue(false)
        mediaPlayer.reset()
        unregisterReceiver(noisyAudioStreamReceiver)
        mediaSession.setPlaybackState(PlaybackStateCompat.Builder().apply {
            setActions(PLAYBACK_ACTION)
            setState(
                PlaybackStateCompat.STATE_STOPPED,
                0L,
                1F
            )
        }.build())
        mediaNotificationManager.stop()
        stopSelf()
    }

    fun pause() {
        if (mediaPlayer.isPlaying) {
            mediaPlayer.pause()
            playbackQueueViewModel.playing.postValue(false)
            mediaSession.setPlaybackState(PlaybackStateCompat.Builder().apply {
                setActions(PLAYBACK_ACTION)
                setState(
                    PlaybackStateCompat.STATE_PAUSED,
                    mediaPlayer.currentPosition.toLong(),
                    1F
                )
            }.build())
            mediaNotificationManager.updatePlay(false)
        }
    }

    fun seekTo(pos: Long) {
        mediaPlayer.seekTo(pos.toInt())
        mediaSession.setPlaybackState(PlaybackStateCompat.Builder().apply {
            setActions(PLAYBACK_ACTION)
            setState(
                if (mediaPlayer.isPlaying) {
                    PlaybackStateCompat.STATE_PLAYING
                } else {
                    PlaybackStateCompat.STATE_PAUSED
                },
                mediaPlayer.currentPosition.toLong(),
                1F
            )
        }.build())
    }

    fun skipToNext() {
        mediaSession.setPlaybackState(PlaybackStateCompat.Builder().apply {
            setActions(PLAYBACK_ACTION)
            setState(
                PlaybackStateCompat.STATE_SKIPPING_TO_NEXT,
                0L,
                1F
            )
        }.build())
        playbackQueueViewModel.nextSong()
    }

    fun skipToPrevious() {
        mediaSession.setPlaybackState(PlaybackStateCompat.Builder().apply {
            setActions(PLAYBACK_ACTION)
            setState(
                PlaybackStateCompat.STATE_SKIPPING_TO_PREVIOUS,
                0L,
                1F
            )
        }.build())
        playbackQueueViewModel.previousSong()
    }

    inner class BecomingNoisyReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == AudioManager.ACTION_AUDIO_BECOMING_NOISY) {
                pause()
            }
        }
    }
}