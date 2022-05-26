package puelloc.musicplayer.ui.dialog

import android.app.Dialog
import android.content.DialogInterface
import android.nfc.NfcAdapter
import android.nfc.NfcAdapter.FLAG_READER_NFC_A
import android.nfc.tech.IsoDep
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import puelloc.musicplayer.R
import puelloc.musicplayer.databinding.DialogNfcBinding
import puelloc.musicplayer.utils.NFCUtils.Companion.APDU_AID
import puelloc.musicplayer.utils.NFCUtils.Companion.selectApdu
import java.io.IOException

class NFCDialog : DialogFragment() {
    private lateinit var nfcAdapter: NfcAdapter

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        nfcAdapter = NfcAdapter.getDefaultAdapter(context)

        val binding = DialogNfcBinding.inflate(requireActivity().layoutInflater)

        nfcAdapter.enableReaderMode(
            activity,
            { tag ->
                val isoDep = IsoDep.get(tag)
                try {
                    isoDep.connect()
                    val res = isoDep.transceive(selectApdu(APDU_AID))
                    val strRes = String(res, Charsets.UTF_8)
                    binding.message.post {
                        binding.message.text = strRes
                    }
                } catch (e: IOException) {
                    binding.message.post {
                        binding.message.text = e.message
                    }
                } finally {
                    isoDep.close()
                }
            },
            FLAG_READER_NFC_A,
            null
        )

        return activity?.let { activity ->
            val dialog = MaterialAlertDialogBuilder(activity)
                .setTitle(R.string.fast_link)
                .setView(binding.root)
                .setNeutralButton(R.string.cancel) { _, _ ->
                    // Do Nothing
                }.setPositiveButton(R.string.ok) { _, _ ->
                    // Do Nothing
                }.create()
            dialog
        } ?: throw IllegalStateException("Activity cannot be null")
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        nfcAdapter.disableReaderMode(activity)
    }
}