package puelloc.musicplayer.ui.fragment

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.selection.*
import puelloc.musicplayer.adapter.SongAdapter
import puelloc.musicplayer.databinding.FragmentSongBinding
import puelloc.musicplayer.entity.Song
import puelloc.musicplayer.viewmodel.MainActivityViewModel
import puelloc.musicplayer.viewmodel.service.MediaPlayState
import puelloc.musicplayer.viewmodel.PlaylistViewModel
import puelloc.musicplayer.viewmodel.SongViewModel


class SongFragment : Fragment() {

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

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentSongBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setHasOptionsMenu(true)
        val adapter = SongAdapter {
            mediaPlayState.song = it
        }
        adapter.setHasStableIds(true)
        binding.musicList.adapter = adapter
        val playlistId = arguments?.getLong(PLAYLIST_ID_BUNDLE_KEY, -1) ?: -1
        if (playlistId == -1L) {
            songViewModel.getSongs().observe(viewLifecycleOwner) {
                adapter.submitList(it)
            }
        } else {
            playlistViewModel.getPlaylistWithSong(playlistId).observe(viewLifecycleOwner) {
                adapter.submitList(it.songs)
            }
            val selectionTracker = SelectionTracker.Builder(
                "my-selection-id",
                binding.musicList,
                object : ItemKeyProvider<Long>(SCOPE_MAPPED) {
                    override fun getKey(position: Int): Long {
                        return adapter.getItemId(position)
                    }

                    override fun getPosition(key: Long): Int {
                        return adapter.currentList.indexOfFirst { it.songId == key }
                    }
                },
                object : ItemDetailsLookup<Long>() {
                    override fun getItemDetails(e: MotionEvent): ItemDetails<Long>? {
                        val childView = binding.musicList.findChildViewUnder(e.x, e.y)
                        if (childView != null) {
                            val viewHolder = binding.musicList.getChildViewHolder(childView)
                            if (viewHolder is SongAdapter.ViewHolder) {
                                return object : ItemDetails<Long>() {
                                    override fun getPosition(): Int {
                                        return viewHolder.bindingAdapterPosition
                                    }

                                    override fun getSelectionKey(): Long {
                                        return viewHolder.itemId
                                    }
                                }
                            }
                        }
                        return null
                    }
                },
                StorageStrategy.createLongStorage()
            ).build()
            adapter.selectionTracker = selectionTracker
            selectionTracker.addObserver(object : SelectionTracker.SelectionObserver<Long>() {
                override fun onItemStateChanged(key: Long, selected: Boolean) {
                    super.onItemStateChanged(key, selected)
                    Log.d(
                        TAG, "$key ${
                            if (selected) {
                                ""
                            } else {
                                "un"
                            }
                        }selected"
                    )
                    mainActivityViewModel.setSelectionSize(selectionTracker.selection.size())
                }
            })
        }
    }
}