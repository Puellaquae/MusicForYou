package puelloc.musicplayer.ui.fragment

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import puelloc.musicplayer.BuildConfig
import puelloc.musicplayer.R
import puelloc.musicplayer.databinding.FragmentForYouBinding
import puelloc.musicplayer.ui.dialog.NFCDialog
import java.util.*

class ForYouFragment : Fragment() {
    private var _binding: FragmentForYouBinding? = null
    private var binding: FragmentForYouBinding
        get() = _binding!!
        set(value) {
            _binding = value
        }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentForYouBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.textView.text =
            getString(R.string.build_time, Date(BuildConfig.BUILD_TIME.toLong()).toString())
        binding.button.setOnClickListener {
            NFCDialog().show(parentFragmentManager, "NFC Dialog")
        }
    }
}