package puelloc.musicplayer.ui.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import puelloc.musicplayer.adapter.SongAdapter
import puelloc.musicplayer.databinding.FragmentSongBinding
import puelloc.musicplayer.viewmodel.SongViewModel

class SongFragment : Fragment() {

    private var _binding: FragmentSongBinding? = null
    private var binding: FragmentSongBinding
        get() = _binding!!
        set(value) {
            _binding = value
        }
    private val viewModel: SongViewModel by activityViewModels()

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
        viewModel.getSongs().observe(viewLifecycleOwner) {
            adapter.submitList(it)
        }
    }
}