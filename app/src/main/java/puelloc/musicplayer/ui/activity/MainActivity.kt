package puelloc.musicplayer.ui.activity

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioManager
import android.os.Bundle
import android.os.RemoteException
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.MediaControllerCompat.*
import android.support.v4.media.session.PlaybackStateCompat
import android.util.Log
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.navigation.NavigationBarView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import puelloc.musicplayer.R
import puelloc.musicplayer.databinding.ActivityMainBinding
import puelloc.musicplayer.service.MediaPlaybackService
import puelloc.musicplayer.service.MediaServiceHelper
import puelloc.musicplayer.ui.fragment.ForYouFragment
import puelloc.musicplayer.ui.fragment.PlaylistFragment
import puelloc.musicplayer.ui.fragment.SongFragment
import puelloc.musicplayer.viewmodel.MediaPlayViewModel
import puelloc.musicplayer.viewmodel.PlaylistViewModel
import puelloc.musicplayer.viewmodel.SongViewModel


private val FRAGMENTS = listOf(
    lazy { ForYouFragment() } to R.id.nav_for_you,
    lazy { SongFragment() } to R.id.nav_song,
    lazy { PlaylistFragment() } to R.id.nav_playlist,
)

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var mediaServiceHelper: MediaServiceHelper
    private val songViewModel: SongViewModel by viewModels()
    private val playlistViewModel: PlaylistViewModel by viewModels()
    private val mediaPlayViewModel: MediaPlayViewModel by viewModels()

    companion object {
        private val TAG = MainActivity::class.java.simpleName
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initView()

        val requestPermissionLauncher = registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) {
            if (it) {
                MainScope().launch(Dispatchers.IO) {
                    songViewModel.loadSongsSync()
                    playlistViewModel.buildPlaylistByDirSync()
                }
            }
        }

        val readCallLogPermissionResult =
            this.checkSelfPermission(android.Manifest.permission.READ_EXTERNAL_STORAGE)
        if (readCallLogPermissionResult != PackageManager.PERMISSION_GRANTED) {
            requestPermissionLauncher.launch(android.Manifest.permission.READ_EXTERNAL_STORAGE)
        } else {
            MainScope().launch(Dispatchers.IO) {
                songViewModel.loadSongsSync()
                playlistViewModel.buildPlaylistByDirSync()
            }
        }

        mediaServiceHelper = object : MediaServiceHelper(this) {
            override fun onConnected(mediaController: MediaControllerCompat) {
                Log.d(TAG, "onConnected")
            }

            override fun onChildrenLoaded(
                parentId: String,
                children: MutableList<MediaBrowserCompat.MediaItem>
            ) {
                Log.d(TAG, "onChildrenLoaded, $parentId, $children")
            }

            override fun onDisconnected() {
                Log.d(TAG, "onDisconnected")
            }
        }
        mediaServiceHelper.registerCallback(controllerCallback)

        mediaPlayViewModel.singId.observe(this) {
            mediaServiceHelper.getTransportControls().play()
        }
    }

    override fun onStart() {
        super.onStart()
        Log.d(TAG, "onStart")
        mediaServiceHelper.start()
    }

    override fun onResume() {
        super.onResume()
        Log.d(TAG, "onResume")
        volumeControlStream = AudioManager.STREAM_MUSIC
    }

    override fun onStop() {
        super.onStop()
        Log.d(TAG, "onStop")
        mediaServiceHelper.stop()
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

    private val controllerCallback = object : MediaControllerCompat.Callback() {
        override fun onMetadataChanged(metadata: MediaMetadataCompat?) {
            Log.d(TAG, "metadata changed, $metadata")
        }

        override fun onPlaybackStateChanged(state: PlaybackStateCompat?) {
            Log.d(TAG, "playback state changed, $state")
        }
    }
}