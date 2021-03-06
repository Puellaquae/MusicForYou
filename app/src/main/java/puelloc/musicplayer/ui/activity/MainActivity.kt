package puelloc.musicplayer.ui.activity

import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.media.AudioManager
import android.os.Bundle
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.util.Log
import android.view.MenuItem
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.view.WindowCompat
import androidx.core.view.forEach
import androidx.fragment.app.Fragment
import androidx.preference.PreferenceManager
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.navigation.NavigationBarView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import puelloc.musicplayer.R
import puelloc.musicplayer.databinding.ActivityMainBinding
import puelloc.musicplayer.enums.AppEvent
import puelloc.musicplayer.service.MediaPlaybackService
import puelloc.musicplayer.service.MediaServiceHelper
import puelloc.musicplayer.trait.IHandleBackPress
import puelloc.musicplayer.trait.IHandleFAB
import puelloc.musicplayer.trait.IHandleMenuItemClick
import puelloc.musicplayer.trait.IHandleNavigationReselect
import puelloc.musicplayer.ui.fragment.ForYouFragment
import puelloc.musicplayer.ui.fragment.MusicLibraryFragment
import puelloc.musicplayer.ui.fragment.PlaybackQueueFragment
import puelloc.musicplayer.ui.fragment.SettingFragment
import puelloc.musicplayer.viewmodel.MainActivityViewModel
import puelloc.musicplayer.viewmodel.PlaybackQueueViewModel
import puelloc.musicplayer.viewmodel.PlaylistViewModel
import puelloc.musicplayer.viewmodel.SongViewModel

class MainActivity : AppCompatActivity(), IHandleMenuItemClick, IHandleFAB,
    IHandleNavigationReselect, SharedPreferences.OnSharedPreferenceChangeListener {
    private lateinit var binding: ActivityMainBinding
    private lateinit var mediaServiceHelper: MediaServiceHelper
    private val songViewModel: SongViewModel by viewModels()
    private val playlistViewModel: PlaylistViewModel by viewModels()
    private val mainActivityViewModel: MainActivityViewModel by viewModels()
    private lateinit var playbackQueueViewModel: PlaybackQueueViewModel

    companion object {
        private val TAG = MainActivity::class.java.simpleName

        val FRAGMENTS = listOf(
            { SettingFragment() } to R.id.nav_for_you,
            { PlaybackQueueFragment() } to R.id.nav_song,
            { MusicLibraryFragment() } to R.id.nav_music_library,
        )

        val BOTTOM_NAVIGATION_ICON = mapOf(
            R.id.nav_for_you to (R.drawable.ic_baseline_star_border_24
                    to R.drawable.ic_baseline_star_24),
            R.id.nav_song to (R.drawable.ic_outline_music_note_24
                    to R.drawable.ic_baseline_music_note_24),
            R.id.nav_music_library to (R.drawable.ic_outline_library_music_24
                    to R.drawable.ic_baseline_library_music_24)
        )

        val MENU_ID_TO_FRAGMENT_INDEX =
            FRAGMENTS.mapIndexed { index, pair -> pair.second to index }.toMap()

        val FRAGMENT_INDEX_TO_MENU_ID =
            FRAGMENTS.mapIndexed { index, pair -> index to pair.second }.toMap()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        Log.d(TAG, "onCreate")
        WindowCompat.setDecorFitsSystemWindows(window, false)
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        playbackQueueViewModel = PlaybackQueueViewModel.getInstance(this.application)

        initView()

        val requestPermissionLauncher = registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) {
            if (it) {
                rebuildDatabase()
            }
        }

        val readCallLogPermissionResult =
            this.checkSelfPermission(android.Manifest.permission.READ_EXTERNAL_STORAGE)
        if (readCallLogPermissionResult != PackageManager.PERMISSION_GRANTED) {
            requestPermissionLauncher.launch(android.Manifest.permission.READ_EXTERNAL_STORAGE)
        } else {
            rebuildDatabase()
        }

        startService(Intent(this, MediaPlaybackService::class.java))

        mediaServiceHelper = object : MediaServiceHelper(this@MainActivity) {
            override fun onConnected(mediaController: MediaControllerCompat) {
                Log.d(TAG, "onConnected, $mediaController")
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
        mediaServiceHelper.create()
        mediaServiceHelper.registerCallback(controllerCallback)

        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)
        sharedPreferences.registerOnSharedPreferenceChangeListener(this)
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
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)
        sharedPreferences.registerOnSharedPreferenceChangeListener(this)
    }

    override fun onPause() {
        super.onPause()
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)
        sharedPreferences.unregisterOnSharedPreferenceChangeListener(this)
    }

    override fun onStop() {
        super.onStop()
        Log.d(TAG, "onStop")
        mediaServiceHelper.stop()
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        binding.bottomNavigation.selectedItemId = R.id.nav_song
    }

    private fun initView() {
        com.google.android.material.appbar.AppBarLayout.ScrollingViewBehavior()

        binding.apply {
            bottomNavigation.apply {
                labelVisibilityMode = NavigationBarView.LABEL_VISIBILITY_SELECTED
                menu.forEach { it.setIcon(BOTTOM_NAVIGATION_ICON[it.itemId]!!.first) }
                setOnItemSelectedListener {
                    if (bottomNavigation.selectedItemId == it.itemId) {
                        onNavigationReselect()
                    } else {
                        menu.forEach { m -> m.setIcon(BOTTOM_NAVIGATION_ICON[m.itemId]!!.first) }
                    }
                    if (MENU_ID_TO_FRAGMENT_INDEX.containsKey(it.itemId)) {
                        it.setIcon(BOTTOM_NAVIGATION_ICON[it.itemId]!!.second)
                        mainActivityViewModel.setCurrentFragmentRes(it.itemId)
                        viewPager.setCurrentItem(
                            MENU_ID_TO_FRAGMENT_INDEX[it.itemId]!!,
                            true
                        )
                        true
                    } else {
                        false
                    }
                }
            }

            viewPager.apply {
                // isUserInputEnabled = false

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
                        val id = FRAGMENT_INDEX_TO_MENU_ID[position]!!
                        binding.bottomNavigation.selectedItemId = id
                    }
                })
            }

            // viewPager.post {
            viewPager.setCurrentItem(MENU_ID_TO_FRAGMENT_INDEX[R.id.nav_song]!!, false)
            bottomNavigation.selectedItemId = R.id.nav_song
            // }

            toolbar.setNavigationOnClickListener {
                onBackPressed()
            }

            toolbar.setOnMenuItemClickListener {
                onMenuItemClicked(it)
            }

            floatingActionButton.setOnClickListener {
                onFABClick()
            }

            mainActivityViewModel.currentTitle.observe(this@MainActivity) {
                toolbar.title = it.first
                toolbar.subtitle = it.second
            }

            mainActivityViewModel.currentTopBarButtonAndMenu.observe(this@MainActivity) {
                if (it.first == null) {
                    toolbar.navigationIcon = null
                } else {
                    toolbar.setNavigationIcon(it.first!!)
                }
                toolbar.menu.clear()
                it.second?.let { menu ->
                    toolbar.inflateMenu(menu)
                }
            }

            mainActivityViewModel.currentFABIcon.observe(this@MainActivity) {
                if (it == null) {
                    floatingActionButton.visibility = View.GONE
                } else {
                    floatingActionButton.visibility = View.VISIBLE
                    floatingActionButton.setImageDrawable(
                        AppCompatResources.getDrawable(
                            this@MainActivity,
                            it
                        )
                    )
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

    private fun rebuildDatabase() {
        if (true) {
            MainScope().launch(Dispatchers.IO) {
                songViewModel.loadSongsSync()
                playlistViewModel.buildPlaylistByDirSync()
            }
        }
    }

    override fun onBackPressed() {
        val currentFragment = getCurrentFragment()
        if (currentFragment is IHandleBackPress &&
            currentFragment.onBackPressed()
        ) {
            return
        }
        super.onBackPressed()
    }

    override fun onMenuItemClicked(menuItem: MenuItem): Boolean {
        val currentFragment = getCurrentFragment()
        if (currentFragment is IHandleMenuItemClick &&
            currentFragment.onMenuItemClicked(menuItem)
        ) {
            return true
        }
        return false
    }

    override fun onFABClick(): Boolean {
        val currentFragment = getCurrentFragment()
        if (currentFragment is IHandleFAB &&
            currentFragment.onFABClick()
        ) {
            return true
        }
        return false
    }

    override fun onNavigationReselect(): Boolean {
        val currentFragment = getCurrentFragment()
        if (currentFragment is IHandleNavigationReselect &&
            currentFragment.onNavigationReselect()
        ) {
            return true
        }
        return false
    }

    private fun getCurrentFragment() =
        supportFragmentManager.findFragmentByTag("f${binding.viewPager.currentItem}")

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        if (key == "play_queue_once") {
            val once = sharedPreferences?.getBoolean(key, false)
            if (once == true) {
                playbackQueueViewModel.emit(AppEvent.PLAY_ONCE_BEGIN_RECORD)
            }
        }
    }
}