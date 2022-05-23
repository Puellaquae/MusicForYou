package puelloc.musicplayer.service

import android.app.Activity
import android.content.ComponentName
import android.content.Context
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.util.Log

abstract class MediaServiceHelper(private val context: Context) {

    companion object {
        private val TAG = MediaServiceHelper::class.java.simpleName
    }

    private lateinit var mediaBrowser: MediaBrowserCompat
    private var mediaController: MediaControllerCompat? = null
    private val callbacks: MutableList<MediaControllerCompat.Callback> = ArrayList()

    private val mediaConnectionCallback = object : MediaBrowserCompat.ConnectionCallback() {
        override fun onConnected() {
            Log.d(TAG, "onConnected")
            mediaController = MediaControllerCompat(context, mediaBrowser.sessionToken)
            MediaControllerCompat.setMediaController(context as Activity, mediaController)
            mediaController!!.registerCallback(mediaControllerCallback)

            mediaControllerCallback.onMetadataChanged(mediaController!!.metadata)
            mediaControllerCallback.onPlaybackStateChanged(mediaController!!.playbackState)

            this@MediaServiceHelper.onConnected(mediaController!!)

            mediaBrowser.subscribe(mediaBrowser.root, mediaBrowserSubscriptionCallback)
        }
    }

    private val mediaControllerCallback = object : MediaControllerCompat.Callback() {
        override fun onMetadataChanged(metadata: MediaMetadataCompat?) {
            preformCallbacks { it.onMetadataChanged(metadata) }
        }

        override fun onPlaybackStateChanged(state: PlaybackStateCompat?) {
            preformCallbacks { it.onPlaybackStateChanged(state) }
        }

        override fun onSessionDestroyed() {
            resetState()
            onPlaybackStateChanged(null)
            this@MediaServiceHelper.onDisconnected()
        }
    }

    private val mediaBrowserSubscriptionCallback =
        object : MediaBrowserCompat.SubscriptionCallback() {
            override fun onChildrenLoaded(
                parentId: String,
                children: MutableList<MediaBrowserCompat.MediaItem>
            ) {
                this@MediaServiceHelper.onChildrenLoaded(parentId, children)
            }
        }

    protected fun getMediaController(): MediaControllerCompat = mediaController!!

    fun getTransportControls(): MediaControllerCompat.TransportControls {
        if (mediaController!!.transportControls == null) {
            Log.w(TAG, "transportControls is null")
        }
        return mediaController!!.transportControls!!
    }

    fun create() {
        Log.d(TAG, "create")
        mediaBrowser = MediaBrowserCompat(
            context,
            ComponentName(context, MediaPlaybackService::class.java),
            mediaConnectionCallback,
            null
        )
    }

    fun start() {
        Log.d(TAG, "start")
        if (!mediaBrowser.isConnected) {
            mediaBrowser.connect()
        }
    }

    fun stop() {
        Log.d(TAG, "stop")
        mediaController?.unregisterCallback(mediaControllerCallback)
        if (mediaBrowser.isConnected) {
            mediaBrowser.disconnect()
        }
        resetState()
    }

    protected abstract fun onConnected(mediaController: MediaControllerCompat)
    protected abstract fun onChildrenLoaded(
        parentId: String,
        children: MutableList<MediaBrowserCompat.MediaItem>
    )

    protected abstract fun onDisconnected()

    private fun resetState() {
        preformCallbacks { it.onPlaybackStateChanged(null) }
    }

    fun registerCallback(callback: MediaControllerCompat.Callback) {
        callbacks.add(callback)
    }

    private fun preformCallbacks(command: (callback: MediaControllerCompat.Callback) -> Unit) {
        callbacks.forEach(command)
    }
}

