package puelloc.musicplayer.ui.activity

import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.os.bundleOf
import androidx.core.view.WindowCompat
import androidx.fragment.app.add
import androidx.fragment.app.commit
import puelloc.musicplayer.App.Companion.PLAYLIST_ID_MESSAGE
import puelloc.musicplayer.databinding.ActivityPlaylistBinding
import puelloc.musicplayer.ui.fragment.SongFragment
import puelloc.musicplayer.ui.fragment.SongFragment.Companion.PLAYLIST_ID_BUNDLE_KEY
import puelloc.musicplayer.viewmodel.PlaylistViewModel

class PlaylistActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPlaylistBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        super.onCreate(savedInstanceState)

        binding = ActivityPlaylistBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val playlistId = intent.getLongExtra(PLAYLIST_ID_MESSAGE, -1)

        val bundle = bundleOf(PLAYLIST_ID_BUNDLE_KEY to playlistId)
        supportFragmentManager.commit {
            setReorderingAllowed(true)
            add<SongFragment>(binding.fragmentContainer.id, args = bundle)
        }

        val playlistViewModel: PlaylistViewModel by viewModels()
        playlistViewModel.getPlaylistWithSong(playlistId).observe(this) {
            binding.toolbar.title = it.playlist.name
            binding.toolbar.subtitle = "${it.songs.size} songs"
        }
    }
}