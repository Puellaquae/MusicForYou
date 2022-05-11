package puelloc.musicplayer.ui.activity

import android.app.Application
import android.content.ComponentName
import android.media.AudioManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.util.Log
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.fragment.app.Fragment
import androidx.media.session.MediaButtonReceiver
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.navigation.NavigationBarView
import puelloc.musicplayer.R
import puelloc.musicplayer.databinding.ActivityMainBinding
import puelloc.musicplayer.observer.AudioObserver
import puelloc.musicplayer.service.MediaPlaybackService
import puelloc.musicplayer.ui.fragment.ForYouFragment
import puelloc.musicplayer.ui.fragment.SongFragment
import puelloc.musicplayer.ui.fragment.PlaylistFragment
import puelloc.musicplayer.viewmodel.SongViewModel

val channelId = ""

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var mediaBrowser: MediaBrowserCompat

    private val connectionCallbacks = object : MediaBrowserCompat.ConnectionCallback() {
        override fun onConnected() {
            mediaBrowser.sessionToken.also {
                val mediaController = MediaControllerCompat(this@MainActivity, it)
                MediaControllerCompat.setMediaController(this@MainActivity, mediaController)
            }
            buildTransportControls()
        }

        override fun onConnectionSuspended() {

        }

        override fun onConnectionFailed() {

        }

        fun buildTransportControls() {
            val mediaController = MediaControllerCompat.getMediaController(this@MainActivity)
            // Grab the view for the play/pause button
            binding.playPause.apply {
                setOnClickListener {
                    // Since this is a play/pause button, you'll need to test the current state
                    // and choose the action accordingly

                    val pbState = mediaController.playbackState.state
                    if (pbState == PlaybackStateCompat.STATE_PLAYING) {
                        mediaController.transportControls.pause()
                    } else {
                        mediaController.transportControls.play()
                    }
                }
            }

            // Display the initial state
            val metadata = mediaController.metadata
            val pbState = mediaController.playbackState

            // Register a Callback to stay in sync
            mediaController.registerCallback(controllerCallback)
        }
    }

    private var controllerCallback = object : MediaControllerCompat.Callback() {

        override fun onMetadataChanged(metadata: MediaMetadataCompat?) {}

        override fun onPlaybackStateChanged(state: PlaybackStateCompat?) {}
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val songViewModel: SongViewModel by viewModels()
        songViewModel.loadSongs()

        initView()

        mediaBrowser = MediaBrowserCompat(
            this,
            ComponentName(this, MediaPlaybackService::class.java),
            connectionCallbacks,
            null
        )
    }

    override fun onStart() {
        super.onStart()
        mediaBrowser.connect()
    }

    override fun onResume() {
        super.onResume()
        volumeControlStream = AudioManager.STREAM_MUSIC
    }

    override fun onStop() {
        super.onStop()
        MediaControllerCompat.getMediaController(this)?.unregisterCallback(controllerCallback)
        mediaBrowser.disconnect()
    }

    private fun initView() {
        binding.apply {
            viewPager.apply {
                isUserInputEnabled = false

                adapter = object : FragmentStateAdapter(this@MainActivity) {
                    override fun getItemCount(): Int = 3

                    override fun createFragment(position: Int): Fragment = when (position) {
                        0 -> ForYouFragment()
                        1 -> SongFragment()
                        2 -> PlaylistFragment()
                        else -> throw RuntimeException()
                    }
                }

                registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
                    override fun onPageSelected(position: Int) {
                        super.onPageSelected(position)
                        binding.bottomNavigation.menu.getItem(position).isChecked = true
                    }
                })
            }

            bottomNavigation.apply {
                labelVisibilityMode = NavigationBarView.LABEL_VISIBILITY_SELECTED
                setOnItemSelectedListener {
                    when (it.itemId) {
                        R.id.nav_song -> binding.viewPager.setCurrentItem(1, true)
                        R.id.nav_for_you -> binding.viewPager.setCurrentItem(0, true)
                        R.id.nav_playlist -> binding.viewPager.setCurrentItem(2, true)
                    }
                    true
                }
                selectedItemId = R.id.nav_song
            }
        }
    }
}