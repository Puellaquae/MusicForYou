package puelloc.musicplayer.ui.fragment

import android.app.Application
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import puelloc.musicplayer.R
import puelloc.musicplayer.adapter.ItemAdapter
import puelloc.musicplayer.databinding.FragmentPlaybackQueueBinding
import puelloc.musicplayer.entity.PlaybackQueueItemWithSong
import puelloc.musicplayer.glide.audiocover.AudioCover
import puelloc.musicplayer.trait.IHandleMenuItemClick
import puelloc.musicplayer.viewmodel.PlaybackQueueViewModel

class PlaybackQueueFragment : Fragment(), IHandleMenuItemClick {

    companion object {
        private const val NEW_PLAYLIST_DIALOG_TAG = "new playlist dialog tag"
    }

    private var _binding: FragmentPlaybackQueueBinding? = null
    private var binding: FragmentPlaybackQueueBinding
        get() = _binding!!
        set(value) {
            _binding = value
        }
    private lateinit var playbackQueueAdapter: ItemAdapter<PlaybackQueueItemWithSong>
    private lateinit var playbackQueueViewModel: PlaybackQueueViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentPlaybackQueueBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        playbackQueueViewModel =
            PlaybackQueueViewModel.getInstance(requireContext().applicationContext as Application)
        playbackQueueAdapter = ItemAdapter(
            { it.queueItem.itemId!! },
            { it.song.name },
            { it.song.artistName },
            { AudioCover(it.song.path) },
            R.drawable.ic_baseline_music_note_24
        ) {
            playbackQueueViewModel.playable.postValue(true)
            playbackQueueViewModel.currentItemId.postValue(it.queueItem.itemId)
        }
        binding.playbackQueueList.adapter = playbackQueueAdapter
        playbackQueueViewModel.playbackQueueWithSong.observe(viewLifecycleOwner) {
            playbackQueueAdapter.submitList(it)
        }
    }

    override fun onMenuItemClicked(menuItem: MenuItem): Boolean {
        return when (menuItem.itemId) {
            R.id.menu_clear_playback_queue -> {
                playbackQueueViewModel.clearQueue()
                true
            }
            else -> false
        }
    }
}