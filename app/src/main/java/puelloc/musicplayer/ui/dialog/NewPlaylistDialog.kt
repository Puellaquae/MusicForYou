package puelloc.musicplayer.ui.dialog

import android.app.Dialog
import android.os.Bundle
import android.text.TextUtils
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import puelloc.musicplayer.R
import puelloc.musicplayer.databinding.DialogNewPlaylistBinding
import puelloc.musicplayer.viewmodel.PlaylistViewModel

class NewPlaylistDialog : DialogFragment() {
    private val playlistViewModel: PlaylistViewModel by activityViewModels()

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        super.onCreateDialog(savedInstanceState)
        return activity?.let {
            val binding = DialogNewPlaylistBinding.inflate(requireActivity().layoutInflater)
            val dialog = MaterialAlertDialogBuilder(it)
                .setTitle(R.string.new_playlist)
                .setView(binding.root)
                .setNeutralButton(R.string.cancel) { _, _ ->
                    // Do Nothing
                }.setPositiveButton(R.string.ok) { _, _ ->
                    val name = binding.nameText.text.toString()
                    if (!TextUtils.isEmpty(name)) {
                        playlistViewModel.newPlaylist(name)
                    }
                }.create()
            dialog
        } ?: throw IllegalStateException("Activity cannot be null")
    }
}