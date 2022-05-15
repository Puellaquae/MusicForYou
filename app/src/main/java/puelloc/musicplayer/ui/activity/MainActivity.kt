package puelloc.musicplayer.ui.activity

import android.content.pm.PackageManager
import android.media.AudioManager
import android.os.Bundle
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.util.Log
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.core.view.forEach
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.navigation.NavigationBarView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import puelloc.musicplayer.R
import puelloc.musicplayer.databinding.ActivityMainBinding
import puelloc.musicplayer.service.MediaServiceHelper
import puelloc.musicplayer.ui.fragment.ForYouFragment
import puelloc.musicplayer.ui.fragment.MusicLibraryFragment
import puelloc.musicplayer.ui.fragment.PlaylistFragment
import puelloc.musicplayer.ui.fragment.SongFragment
import puelloc.musicplayer.viewmodel.MainActivityViewModel
import puelloc.musicplayer.viewmodel.MainActivityViewModel.Companion.SHOW_MUSIC_LIBRARY
import puelloc.musicplayer.viewmodel.service.MediaPlayState
import puelloc.musicplayer.viewmodel.PlaylistViewModel
import puelloc.musicplayer.viewmodel.SongViewModel


class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var mediaServiceHelper: MediaServiceHelper
    private val songViewModel: SongViewModel by viewModels()
    private val playlistViewModel: PlaylistViewModel by viewModels()
    private val mainActivityViewModel: MainActivityViewModel by viewModels()
    private val mediaPlayState = MediaPlayState.getInstance()

    companion object {
        private val TAG = MainActivity::class.java.simpleName

        val FRAGMENTS = listOf(
            { ForYouFragment() } to R.id.nav_for_you,
            { SongFragment() } to R.id.nav_song,
            { MusicLibraryFragment() } to R.id.nav_playlist,
        )

        val BOTTOM_NAVIGATION_ICON = mapOf(
            R.id.nav_for_you to (R.drawable.ic_baseline_star_border_24
                    to R.drawable.ic_baseline_star_24),
            R.id.nav_song to (R.drawable.ic_outline_music_note_24
                    to R.drawable.ic_baseline_music_note_24),
            R.id.nav_playlist to (R.drawable.ic_outline_library_music_24
                    to R.drawable.ic_baseline_library_music_24)
        )

        val MENU_ID_TO_FRAGMENT_INDEX =
            FRAGMENTS.mapIndexed { index, pair -> pair.second to index }.toMap()
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

        mediaPlayState.registerSongListener {
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
                        in 0..FRAGMENTS.size -> FRAGMENTS[position].first()
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
                menu.forEach { it.setIcon(BOTTOM_NAVIGATION_ICON[it.itemId]!!.first) }
                setOnItemSelectedListener {
                    if (bottomNavigation.selectedItemId == it.itemId) {
                        if (it.itemId == R.id.nav_playlist) {
                            mainActivityViewModel.setToShowPlaylist(SHOW_MUSIC_LIBRARY)
                        }
                    } else {
                        menu.forEach { m -> m.setIcon(BOTTOM_NAVIGATION_ICON[m.itemId]!!.first) }
                    }
                    if (MENU_ID_TO_FRAGMENT_INDEX.containsKey(it.itemId)) {
                        it.setIcon(BOTTOM_NAVIGATION_ICON[it.itemId]!!.second)
                        mainActivityViewModel.setCurrentFragmentRes(it.itemId)
                        true
                    } else {
                        false
                    }
                }
            }

            viewPager.post {
                bottomNavigation.selectedItemId = R.id.nav_song
            }

            mainActivityViewModel.currentMusicLibraryTitle.observe(this@MainActivity) {
                toolbar.title = it.first
                toolbar.subtitle = it.second
            }

            mainActivityViewModel.currentFragmentRes.observe(this@MainActivity) {
                val idx = MENU_ID_TO_FRAGMENT_INDEX[it]
                if (idx != null) {
                    viewPager.setCurrentItem(idx, true)
                }
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

    override fun onBackPressed() {
        if (binding.bottomNavigation.selectedItemId == R.id.nav_playlist) {
            if (mainActivityViewModel.showPlaylistId.value != SHOW_MUSIC_LIBRARY) {
                mainActivityViewModel.setToShowPlaylist(SHOW_MUSIC_LIBRARY)
            } else {
                super.onBackPressed()
            }
        } else {
            super.onBackPressed()
        }
    }
}