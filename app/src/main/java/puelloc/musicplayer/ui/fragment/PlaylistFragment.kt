package puelloc.musicplayer.ui.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import puelloc.musicplayer.adapter.PlaylistAdapter
import puelloc.musicplayer.databinding.FragmentPlaylistBinding
import puelloc.musicplayer.viewmodel.MainActivityViewModel
import puelloc.musicplayer.viewmodel.PlaylistViewModel

class PlaylistFragment : Fragment() {
    private var _binding: FragmentPlaylistBinding? = null
    private var binding: FragmentPlaylistBinding
        get() = _binding!!
        set(value) {
            _binding = value
        }
    private val viewModel: PlaylistViewModel by activityViewModels()
    private val mainActivityViewModel: MainActivityViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentPlaylistBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val adapter = PlaylistAdapter {
            mainActivityViewModel.setToShowPlaylist(it.playlist.playlistId!!)
        }
        binding.playlistList.adapter = adapter
        viewModel.playlistsWithSongs.observe(viewLifecycleOwner) {
            adapter.submitList(it)
        }
    }
}