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
import puelloc.musicplayer.adapter.ReorderableItemAdapter
import puelloc.musicplayer.databinding.FragmentPlaybackQueueBinding
import puelloc.musicplayer.glide.audiocover.AudioCover
import puelloc.musicplayer.pojo.relation.PlaybackQueueItemWithSong
import puelloc.musicplayer.trait.IHandleMenuItemClick
import puelloc.musicplayer.ui.dialog.PickPlaylistDialog
import puelloc.musicplayer.ui.dialog.PickPlaylistDialog.Companion.PICK_PLAYLIST_DIALOG_TAG
import puelloc.musicplayer.viewmodel.PlaybackQueueViewModel

class PlaybackQueueFragment : Fragment(), IHandleMenuItemClick {
    private var _binding: FragmentPlaybackQueueBinding? = null
    private var binding: FragmentPlaybackQueueBinding
        get() = _binding!!
        set(value) {
            _binding = value
        }
    private lateinit var playbackQueueAdapter: ItemAdapter<PlaybackQueueItemWithSong>
    private lateinit var playbackQueueViewModel: PlaybackQueueViewModel
    private var lastPlayItemId: Long = -1L

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
        playbackQueueAdapter = object : ReorderableItemAdapter<PlaybackQueueItemWithSong>(
            binding.playbackQueueList,
            { it.queueItem.itemId!! },
            { it.song.name },
            { it.song.artistName },
            { AudioCover(it.song.path) },
            R.drawable.ic_baseline_music_note_24,
            {
                playbackQueueViewModel.playing.postValue(true)
                playbackQueueViewModel.currentItemId.postValue(it.queueItem.itemId)
            }
        ) {
            override fun onBindViewHolder(holder: ViewHolder, position: Int) {
                val item = getItem(position)
                holder.bind(item, item.queueItem.itemId == lastPlayItemId)
            }

            override fun onBindViewHolder(
                holder: ViewHolder,
                position: Int,
                payloads: MutableList<Any>
            ) {
                val item = getItem(position)
                holder.bind(item, item.queueItem.itemId == lastPlayItemId)
            }

            override fun swap(fromPosition: Int, toPosition: Int) {
                val fromItem = getItem(fromPosition)
                val toItem = getItem(toPosition)
                playbackQueueViewModel.moveItemAndInsert(fromItem.queueItem, toItem.queueItem.order, fromPosition > toPosition)
            }
        }
        binding.playbackQueueList.adapter = playbackQueueAdapter
        playbackQueueViewModel.playbackQueueWithSong.observe(viewLifecycleOwner) {
            playbackQueueAdapter.submitList(it)
        }
        playbackQueueViewModel.currentItemId.observe(viewLifecycleOwner) { itemId ->
            val lastPosition =
                playbackQueueAdapter.currentList.indexOfFirst {
                    it.queueItem.itemId == lastPlayItemId
                }
            lastPlayItemId = itemId
            val nextPosition =
                playbackQueueAdapter.currentList.indexOfFirst { it.queueItem.itemId == itemId }
            if (lastPosition != -1) {
                playbackQueueAdapter.notifyItemChanged(lastPosition, false)
            }
            if (nextPosition != -1) {
                playbackQueueAdapter.notifyItemChanged(nextPosition, true)
            }
        }
    }

    override fun onMenuItemClicked(menuItem: MenuItem): Boolean {
        return when (menuItem.itemId) {
            R.id.menu_clear_playback_queue -> {
                playbackQueueViewModel.clearQueue()
                true
            }
            R.id.menu_save_to_playlist -> {
                val pickPlaylistDialog = PickPlaylistDialog {
                    playbackQueueViewModel.saveToPlaylist(it)
                }
                pickPlaylistDialog.show(
                    parentFragmentManager,
                    PICK_PLAYLIST_DIALOG_TAG
                )
                true
            }
            else -> false
        }
    }
}