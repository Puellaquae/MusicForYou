package puelloc.musicplayer.ui.fragment

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import puelloc.musicplayer.databinding.FragmentForYouBinding

class ForYouFragment : Fragment() {
    private var _binding: FragmentForYouBinding? = null;
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
}