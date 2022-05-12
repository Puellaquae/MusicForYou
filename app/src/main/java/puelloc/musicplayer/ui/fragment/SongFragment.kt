package puelloc.musicplayer.ui.fragment

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import puelloc.musicplayer.adapter.SongAdapter
import puelloc.musicplayer.databinding.FragmentSongBinding
import puelloc.musicplayer.viewmodel.PlaylistViewModel
import puelloc.musicplayer.viewmodel.SongViewModel

const val PLAYLIST_ID_BUNDLE_KEY = "playlistId"

class SongFragment : Fragment() {

    private var _binding: FragmentSongBinding? = null
    private var binding: FragmentSongBinding
        get() = _binding!!
        set(value) {
            _binding = value
        }
    private val songViewModel: SongViewModel by activityViewModels()
    private val playlistViewModel: PlaylistViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentSongBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val adapter = SongAdapter()
        binding.musicList.adapter = adapter
        val playlistId = arguments?.getLong(PLAYLIST_ID_BUNDLE_KEY, -1) ?: -1
        if (playlistId == -1L) {
            songViewModel.getSongs().observe(requireActivity()) {
                adapter.submitList(it)
            }
        } else {
            playlistViewModel.getPlaylistWithSong(playlistId).observe(requireActivity()) {
                adapter.submitList(it.songs)
            }
        }
    }
}