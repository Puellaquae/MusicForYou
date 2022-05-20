package puelloc.musicplayer.ui.fragment

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.selection.SelectionTracker
import puelloc.musicplayer.R
import puelloc.musicplayer.adapter.SelectableItemAdapter
import puelloc.musicplayer.databinding.FragmentSongBinding
import puelloc.musicplayer.entity.Song
import puelloc.musicplayer.glide.audiocover.AudioCover
import puelloc.musicplayer.trait.IHandleBackPress
import puelloc.musicplayer.trait.IHandleMenuItemClick
import puelloc.musicplayer.viewmodel.MainActivityViewModel
import puelloc.musicplayer.viewmodel.PlaylistViewModel
import puelloc.musicplayer.viewmodel.SongViewModel
import puelloc.musicplayer.viewmodel.service.MediaPlayState


class SongFragment : Fragment(), IHandleBackPress, IHandleMenuItemClick {

    companion object {
        const val PLAYLIST_ID_BUNDLE_KEY = "playlistId"
        private val TAG = SongFragment::class.java.simpleName
    }

    private var _binding: FragmentSongBinding? = null
    private var binding: FragmentSongBinding
        get() = _binding!!
        set(value) {
            _binding = value
        }
    private val songViewModel: SongViewModel by activityViewModels()
    private val playlistViewModel: PlaylistViewModel by activityViewModels()
    private val mainActivityViewModel: MainActivityViewModel by activityViewModels()
    private val mediaPlayState = MediaPlayState.getInstance()
    private lateinit var songAdapter: SelectableItemAdapter<Song>

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentSongBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        songAdapter = SelectableItemAdapter(
            binding.musicList,
            { it.songId },
            { it.name },
            { it.artistName },
            { AudioCover(it.path) },
            R.drawable.ic_baseline_music_note_24
        ) {
            mediaPlayState.song = it
        }
        val playlistId = arguments?.getLong(PLAYLIST_ID_BUNDLE_KEY, -1) ?: -1
        if (playlistId == -1L) {
            songViewModel.getSongs().observe(viewLifecycleOwner) {
                songAdapter.submitList(it)
            }
        } else {
            playlistViewModel.getPlaylistWithSong(playlistId).observe(viewLifecycleOwner) {
                songAdapter.submitList(it.songs)
            }
            songAdapter.addSelectionObserver(object : SelectionTracker.SelectionObserver<Long>() {
                override fun onItemStateChanged(key: Long, selected: Boolean) {
                    super.onItemStateChanged(key, selected)
                    mainActivityViewModel.setPlaylistSongsSelectionSize(songAdapter.selectionCount())
                }
            })
        }
    }

    override fun onBackPressed(): Boolean {
        if (songAdapter.hasSelection()) {
            songAdapter.clearSelection()
            return true
        }
        return false
    }

    override fun onMenuItemClicked(menuItem: MenuItem): Boolean {
        return when (menuItem.itemId) {
            R.id.selection_select_all -> {
                binding.musicList.post { songAdapter.selectAll() }
                true
            }
            R.id.selection_add_to_play_queue -> {
                true
            }
            else -> false
        }
    }
}