package puelloc.musicplayer.ui.fragment

import android.content.res.Resources
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.color.MaterialColors
import puelloc.musicplayer.databinding.FragmentMiniPlayerBinding

class MiniPlayerFragment: BottomSheetDialogFragment() {
    companion object {
        private val TAG = this::class.java.declaringClass.simpleName
    }

    private lateinit var binding: FragmentMiniPlayerBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentMiniPlayerBinding.inflate(inflater, container, false)
        return binding.root
    }
}