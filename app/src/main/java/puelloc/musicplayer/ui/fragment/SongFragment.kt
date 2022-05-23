package puelloc.musicplayer.ui.fragment

import android.os.Bundle
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
import puelloc.musicplayer.ui.dialog.PickPlaylistDialog
import puelloc.musicplayer.viewmodel.MainActivityViewModel
import puelloc.musicplayer.viewmodel.PlaybackQueueViewModel
import puelloc.musicplayer.viewmodel.PlaylistViewModel
import puelloc.musicplayer.viewmodel.SongViewModel
import kotlin.properties.Delegates

class SongFragment : Fragment(), IHandleBackPress, IHandleMenuItemClick {

    companion object {
        const val PLAYLIST_ID_BUNDLE_KEY = "playlistId"
        const val SHOW_PLAY_QUEUE = -1L
        const val PICK_PLAYLIST_DIALOG_TAG = "pick playlist dialog tag"
        private val TAG = SongFragment::class.java.simpleName
    }

    private var _binding: FragmentSongBinding? = null
    private var binding: FragmentSongBinding
        get() = _binding!!
        set(value) {
            _binding = value
        }
    private var currentPlaylistId by Delegates.notNull<Long>()
    private val songViewModel: SongViewModel by activityViewModels()
    private val playlistViewModel: PlaylistViewModel by activityViewModels()
    private val playbackQueueViewModel: PlaybackQueueViewModel by activityViewModels()
    private val mainActivityViewModel: MainActivityViewModel by activityViewModels()
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
            TODO("Save current playback queue, play queue start with the song")
        }
        currentPlaylistId =
            arguments?.getLong(PLAYLIST_ID_BUNDLE_KEY, SHOW_PLAY_QUEUE) ?: SHOW_PLAY_QUEUE
        if (currentPlaylistId == SHOW_PLAY_QUEUE) {
            songViewModel.getSongs().observe(viewLifecycleOwner) {
                songAdapter.submitList(it)
            }
        } else {
            playlistViewModel.getPlaylistWithSong(currentPlaylistId).observe(viewLifecycleOwner) {
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
                playbackQueueViewModel.appendSongs(songAdapter.getSelection())
                songAdapter.clearSelection()
                true
            }
            R.id.selection_add_to_playlist -> {
                val pickPlaylistDialog = PickPlaylistDialog(currentPlaylistId) {
                    playlistViewModel.addSongsBySongIdToPlaylistWithPlaylistId(
                        it,
                        songAdapter.getSelection()
                    )
                    songAdapter.clearSelection()
                }
                pickPlaylistDialog.show(
                    parentFragmentManager,
                    PICK_PLAYLIST_DIALOG_TAG
                )
                true
            }
            R.id.selection_remove_from_playlist -> {
                playlistViewModel.removeSongsBySongIdFromPlaylistWithPlaylistId(
                    currentPlaylistId,
                    songAdapter.getSelection()
                )
                songAdapter.clearSelection()
                true
            }
            else -> false
        }
    }
}