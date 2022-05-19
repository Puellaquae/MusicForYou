package puelloc.musicplayer.service

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

    private var mediaBrowser: MediaBrowserCompat? = null
    private var mediaController: MediaControllerCompat? = null
    private val callbacks: MutableList<MediaControllerCompat.Callback> = ArrayList()

    private val mediaConnectionCallback = object : MediaBrowserCompat.ConnectionCallback() {
        override fun onConnected() {
            Log.d(TAG, "onConnected")
            mediaController = MediaControllerCompat(context, mediaBrowser!!.sessionToken)
            mediaController!!.registerCallback(mediaControllerCallback)

            mediaControllerCallback.onMetadataChanged(mediaController!!.metadata)
            mediaControllerCallback.onPlaybackStateChanged(mediaController!!.playbackState)

            this@MediaServiceHelper.onConnected(mediaController!!)

            mediaBrowser
            mediaBrowser!!.subscribe(mediaBrowser!!.root, mediaBrowserSubscriptionCallback)
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
        if (mediaController == null) {
            Log.w(TAG, "mediaController is null")
        }
        if (mediaController!!.transportControls == null) {
            Log.w(TAG, "transportControls is null")
        }
        return mediaController!!.transportControls!!
    }

    fun start() {
        if (mediaBrowser == null) {
            mediaBrowser = MediaBrowserCompat(
                context,
                ComponentName(context, MediaPlaybackService::class.java),
                mediaConnectionCallback,
                null
            )
            mediaBrowser!!.connect()
        }
    }

    fun stop() {
        if (mediaController != null) {
            mediaController!!.unregisterCallback(mediaControllerCallback)
            mediaController = null
        }
        if (mediaBrowser != null && mediaBrowser!!.isConnected) {
            mediaBrowser!!.disconnect()
            mediaBrowser = null
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

        mediaController?.apply {
            metadata?.let {
                callback.onMetadataChanged(it)
            }
            playbackState?.let {
                callback.onPlaybackStateChanged(it)
            }
        }
    }

    private fun preformCallbacks(command: (callback: MediaControllerCompat.Callback) -> Unit) {
        callbacks.forEach(command)
    }
}

