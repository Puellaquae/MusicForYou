package puelloc.musicplayer.ui.dialog

import android.app.Dialog
import android.os.Bundle
import android.text.TextUtils
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import puelloc.musicplayer.R
import puelloc.musicplayer.adapter.ItemAdapter
import puelloc.musicplayer.databinding.DialogPickPlaylistBinding
import puelloc.musicplayer.pojo.relation.PlaylistWithSongs
import puelloc.musicplayer.glide.audiocover.AudioCover
import puelloc.musicplayer.ui.viewholder.SimpleItemViewHolder
import puelloc.musicplayer.viewmodel.PlaylistViewModel

class PickPlaylistDialog(
    private val excludePlaylistId: Long = NO_EXCLUDE_PLAYLIST_ID,
    private val callBack: (playlistId: Long) -> Unit
) : DialogFragment() {
    companion object {
        const val NO_EXCLUDE_PLAYLIST_ID = -1L
        const val PICK_PLAYLIST_DIALOG_TAG = "pick playlist dialog tag"
    }

    private val playlistViewModel: PlaylistViewModel by activityViewModels()

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        super.onCreateDialog(savedInstanceState)
        return activity?.let { activity ->
            val binding = DialogPickPlaylistBinding.inflate(requireActivity().layoutInflater)
            val adapter = ItemAdapter(
                { it.playlist.playlistId!! },
                SimpleItemViewHolder<PlaylistWithSongs>(
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
                    callBack(it.playlist.playlistId!!)
                    dismiss()
                }
            )
            binding.playlistList.adapter = adapter
            playlistViewModel.getAllNotFromFolderPlaylistsWithSongs().observe(this) { list ->
                if (excludePlaylistId != NO_EXCLUDE_PLAYLIST_ID) {
                    adapter.submitList(list.filter { it.playlist.playlistId != excludePlaylistId })
                } else {
                    adapter.submitList(list)
                }
            }
            val dialog = MaterialAlertDialogBuilder(activity)
                .setTitle(R.string.pick_playlist)
                .setView(binding.root)
                .setNeutralButton(R.string.cancel, null)
                .setPositiveButton(R.string.ok) { _, _ ->
                    val name = binding.nameText.text.toString()
                    if (!TextUtils.isEmpty(name)) {
                        playlistViewModel.newPlaylistAndGetPlaylistId(name)
                            .observe(this) {
                                callBack(it)
                            }
                    } else {
                        playlistViewModel.newPlaylistAndGetPlaylistId()
                            .observe(this) {
                                callBack(it)
                            }
                    }
                }.create()
            dialog
        } ?: throw IllegalStateException("Activity cannot be null")
    }
}