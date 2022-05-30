package puelloc.musicplayer.ui.dialog

import android.app.Dialog
import android.content.Intent
import android.os.Build.VERSION_CODES.Q
import android.os.Bundle
import androidx.annotation.RequiresApi
import androidx.fragment.app.DialogFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import puelloc.musicplayer.R
import puelloc.musicplayer.databinding.DialogNfcBinding
import puelloc.musicplayer.service.AudioCaptureService

@RequiresApi(Q)
class PlaybackCaptureDialog(val data: Intent) : DialogFragment() {
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val binding = DialogNfcBinding.inflate(requireActivity().layoutInflater)

        activity.let {
            requireContext().startService(
                Intent(
                    requireContext(),
                    AudioCaptureService::class.java
                ).apply {
                    action = AudioCaptureService.ACTION_START
                    putExtra(AudioCaptureService.EXTRA_RESULT_DATA, this@PlaybackCaptureDialog.data)
                })
        }

        binding.message.text = "Capturing"

        return activity?.let { activity ->
            val dialog = MaterialAlertDialogBuilder(activity)
                .setTitle("Playback Capture")
                .setView(binding.root)
                .setNeutralButton(R.string.cancel) { _, _ ->
                    requireContext().startService(
                        Intent(
                            requireContext(),
                            AudioCaptureService::class.java
                        ).apply { action = AudioCaptureService.ACTION_STOP })
                }.setPositiveButton(R.string.ok) { _, _ ->
                    requireContext().startService(
                        Intent(
                            requireContext(),
                            AudioCaptureService::class.java
                        ).apply { action = AudioCaptureService.ACTION_STOP })
                }.create()
            dialog
        } ?: throw IllegalStateException("Activity cannot be null")
    }
}