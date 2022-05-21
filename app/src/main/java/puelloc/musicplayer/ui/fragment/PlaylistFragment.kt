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
import puelloc.musicplayer.databinding.FragmentPlaylistBinding
import puelloc.musicplayer.entity.PlaylistWithSongs
import puelloc.musicplayer.glide.audiocover.AudioCover
import puelloc.musicplayer.trait.IHandleBackPress
import puelloc.musicplayer.trait.IHandleFAB
import puelloc.musicplayer.trait.IHandleMenuItemClick
import puelloc.musicplayer.ui.dialog.NewPlaylistDialog
import puelloc.musicplayer.viewmodel.MainActivityViewModel
import puelloc.musicplayer.viewmodel.PlaylistViewModel

class PlaylistFragment : Fragment(), IHandleBackPress, IHandleFAB, IHandleMenuItemClick {

    companion object {
        private const val NEW_PLAYLIST_DIALOG_TAG = "new playlist dialog tag"
    }

    private var _binding: FragmentPlaylistBinding? = null
    private var binding: FragmentPlaylistBinding
        get() = _binding!!
        set(value) {
            _binding = value
        }
    private val playlistViewModel: PlaylistViewModel by activityViewModels()
    private val mainActivityViewModel: MainActivityViewModel by activityViewModels()
    private lateinit var playlistAdapter: SelectableItemAdapter<PlaylistWithSongs>

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentPlaylistBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        playlistAdapter = SelectableItemAdapter(
            binding.playlistList,
            { it.playlist.playlistId!! },
            { it.playlist.name },
            { getString(R.string.songs_count, it.songs.size) },
            {
                if (it.songs.isEmpty()) {
                    R.drawable.ic_baseline_music_note_24
                } else {
                    AudioCover(it.songs.first().path)
                }
            },
            R.drawable.ic_baseline_music_note_24
        ) {
            mainActivityViewModel.setToShowPlaylist(it.playlist.playlistId!!)
        }
        playlistViewModel.playlistsWithSongs.observe(viewLifecycleOwner) {
            playlistAdapter.submitList(it)
        }
        playlistAdapter.addSelectionObserver(object : SelectionTracker.SelectionObserver<Long>() {
            override fun onItemStateChanged(key: Long, selected: Boolean) {
                super.onItemStateChanged(key, selected)
                mainActivityViewModel.setPlaylistsSelectionSize(playlistAdapter.selectionCount())
            }
        })
    }

    override fun onBackPressed(): Boolean {
        if (playlistAdapter.hasSelection()) {
            playlistAdapter.clearSelection()
            return true
        }
        return false
    }

    override fun onFABClick(): Boolean {
        val newPlaylistDialog = NewPlaylistDialog()
        newPlaylistDialog.show(parentFragmentManager, NEW_PLAYLIST_DIALOG_TAG)
        return true
    }

    override fun onMenuItemClicked(menuItem: MenuItem): Boolean {
        return when (menuItem.itemId) {
            R.id.menu_select_all -> {
                binding.playlistList.post { playlistAdapter.selectAll() }
                true
            }
            R.id.menu_delete_playlist -> {
                playlistViewModel.deletePlaylistsByPlaylistId(playlistAdapter.getSelection(), false)
                playlistAdapter.clearSelection()
                true
            }
            else -> false
        }
    }
}