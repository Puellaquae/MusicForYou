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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import puelloc.musicplayer.R
import puelloc.musicplayer.databinding.ActivityMainBinding
import puelloc.musicplayer.observer.AudioObserver
import puelloc.musicplayer.service.MediaPlaybackService
import puelloc.musicplayer.ui.fragment.ForYouFragment
import puelloc.musicplayer.ui.fragment.SongFragment
import puelloc.musicplayer.ui.fragment.PlaylistFragment
import puelloc.musicplayer.viewmodel.PlaylistViewModel
import puelloc.musicplayer.viewmodel.SongViewModel

private val FRAGMENTS = listOf(
    lazy { ForYouFragment() } to R.id.nav_for_you,
    lazy { SongFragment() } to R.id.nav_song,
    lazy { PlaylistFragment() } to R.id.nav_playlist,
)

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var mediaBrowser: MediaBrowserCompat

    private val connectionCallbacks = object : MediaBrowserCompat.ConnectionCallback() {
        override fun onConnected() {
            Log.d("$this", "Connected")
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

            // Display the initial state
            val metadata = mediaController.metadata
            val pbState = mediaController.playbackState

            // Register a Callback to stay in sync
            mediaController.registerCallback(controllerCallback)
        }
    }

    private var controllerCallback = object : MediaControllerCompat.Callback() {

        override fun onMetadataChanged(metadata: MediaMetadataCompat?) {
            Log.d("controllerCallback@onMetadataChanged", "$metadata")
        }

        override fun onPlaybackStateChanged(state: PlaybackStateCompat?) {
            Log.d("controllerCallback@onPlaybackStateChanged", "$state")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val songViewModel: SongViewModel by viewModels()
        val playlistViewModel: PlaylistViewModel by viewModels()

        MainScope().launch(Dispatchers.IO) {
            songViewModel.loadSongsSync()
            playlistViewModel.buildPlaylistByDirSync()
        }

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
                    override fun getItemCount(): Int = FRAGMENTS.size

                    override fun createFragment(position: Int): Fragment = when (position) {
                        in 0..FRAGMENTS.size -> FRAGMENTS[position].first.value
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
                    val idx = FRAGMENTS.indexOfFirst { f -> f.second == it.itemId }
                    if (idx != -1) {
                        binding.viewPager.setCurrentItem(idx, true)
                        true
                    } else {
                        false
                    }
                }
                selectedItemId = R.id.nav_song
            }
        }
    }
}