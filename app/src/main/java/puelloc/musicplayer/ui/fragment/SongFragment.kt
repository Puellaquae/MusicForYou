package puelloc.musicplayer.ui.fragment

import android.app.Application
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
import puelloc.musicplayer.trait.IHandleFAB
import puelloc.musicplayer.trait.IHandleMenuItemClick
import puelloc.musicplayer.ui.dialog.MiniPlayerDialog
import puelloc.musicplayer.ui.dialog.PickPlaylistDialog
import puelloc.musicplayer.ui.dialog.PickPlaylistDialog.Companion.PICK_PLAYLIST_DIALOG_TAG
import puelloc.musicplayer.ui.viewholder.SimpleItemViewHolder
import puelloc.musicplayer.viewmodel.MainActivityViewModel
import puelloc.musicplayer.viewmodel.MainActivityViewModel.Companion.MUSIC_LIBRARY_SHOW_PLAYLISTS
import puelloc.musicplayer.viewmodel.PlaybackQueueViewModel
import puelloc.musicplayer.viewmodel.PlaylistViewModel
import kotlin.properties.Delegates

class SongFragment : Fragment(), IHandleBackPress, IHandleMenuItemClick, IHandleFAB {

    companion object {
        const val PLAYLIST_ID_BUNDLE_KEY = "playlistId"
        const val UNREACHABLE = -1L
        private const val TAG = "SongFragment"
    }

    private var _binding: FragmentSongBinding? = null
    private var binding: FragmentSongBinding
        get() = _binding!!
        set(value) {
            _binding = value
        }
    private var currentPlaylistId by Delegates.notNull<Long>()
    private val playlistViewModel: PlaylistViewModel by activityViewModels()
    private lateinit var playbackQueueViewModel: PlaybackQueueViewModel
    private val mainActivityViewModel: MainActivityViewModel by activityViewModels()
    private lateinit var songAdapter: SelectableItemAdapter<Song, SimpleItemViewHolder<Song>.ViewHolder>

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentSongBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        playbackQueueViewModel =
            PlaybackQueueViewModel.getInstance(requireContext().applicationContext as Application)
        songAdapter = SelectableItemAdapter(
            binding.musicList,
            { it.songId },
            SimpleItemViewHolder(
                { it.name },
                { it.artistName },
                { AudioCover(it.path) },
                R.drawable.ic_baseline_music_note_24
            ) { song ->
                if (true) {
                    val miniPlayerDialog = MiniPlayerDialog(song)
                    miniPlayerDialog.show(parentFragmentManager, "MiniPlayerDialog")
                } else {
                    playbackQueueViewModel.playPlaylist(
                        songAdapter.currentList.map { it.songId },
                        song.songId
                    )
                }
            }
        )
        currentPlaylistId =
            arguments?.getLong(PLAYLIST_ID_BUNDLE_KEY, UNREACHABLE) ?: UNREACHABLE
        if (currentPlaylistId == UNREACHABLE) {
            throw IllegalStateException()
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
            R.id.menu_select_all, R.id.selection_select_all -> {
                binding.musicList.post { songAdapter.selectAll() }
                true
            }
            R.id.selection_add_to_play_queue -> {
                binding.musicList.post {
                    playbackQueueViewModel.appendSongs(songAdapter.getSelection())
                    songAdapter.clearSelection()
                }
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
                binding.musicList.post {
                    playlistViewModel.removeSongsBySongIdFromPlaylistWithPlaylistId(
                        currentPlaylistId,
                        songAdapter.getSelection()
                    )
                    songAdapter.clearSelection()
                }
                true
            }
            R.id.menu_delete_playlist -> {
                playlistViewModel.deletePlaylistsByPlaylistId(listOf(currentPlaylistId))
                mainActivityViewModel.setToShowPlaylist(MUSIC_LIBRARY_SHOW_PLAYLISTS)
                true
            }
            else -> false
        }
    }

    override fun onFABClick(): Boolean {
        if (songAdapter.hasSelection()) {
            playbackQueueViewModel.appendSongs(songAdapter.getSelection())
            songAdapter.clearSelection()
        } else if (songAdapter.currentList.isNotEmpty()) {
            playbackQueueViewModel.playPlaylist(
                songAdapter.currentList.map { it.songId },
                songAdapter.currentList.first().songId
            )
        }
        return true
    }
}