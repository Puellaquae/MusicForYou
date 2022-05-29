package puelloc.musicplayer.ui.fragment

import android.app.Application
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import puelloc.musicplayer.BuildConfig
import puelloc.musicplayer.R
import puelloc.musicplayer.adapter.ItemAdapter
import puelloc.musicplayer.databinding.FragmentForYouBinding
import puelloc.musicplayer.pojo.relation.PlaybackQueueItemWithSong
import puelloc.musicplayer.ui.dialog.NFCDialog
import puelloc.musicplayer.viewmodel.PlaybackQueueViewModel
import java.util.*

class ForYouFragment : Fragment() {
    private var _binding: FragmentForYouBinding? = null
    private var binding: FragmentForYouBinding
        get() = _binding!!
        set(value) {
            _binding = value
        }

    private lateinit var playbackQueueViewModel: PlaybackQueueViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentForYouBinding.inflate(inflater, container, false)
        playbackQueueViewModel = PlaybackQueueViewModel.getInstance(requireContext().applicationContext as Application)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.textView.text =
            getString(R.string.build_time, Date(BuildConfig.BUILD_TIME.toLong()).toString())
        binding.button.setOnClickListener {
            NFCDialog().show(parentFragmentManager, "NFC Dialog")
        }
        val adapter = ItemAdapter<PlaybackQueueItemWithSong>(
            { it.queueItem.itemId!! },
            { "item#${it.queueItem.itemId!!}, order#${it.queueItem.order}" },
            { "song#${it.song.name}" },
            { R.drawable.ic_baseline_music_note_24 },
            R.drawable.ic_baseline_music_note_24
        ) { }
        binding.rawDataList.adapter = adapter
        playbackQueueViewModel.playbackQueueWithSong.observe(viewLifecycleOwner) {
            adapter.submitList(it)
        }
    }
}