package puelloc.musicplayer.ui.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.*
import puelloc.musicplayer.databinding.FragmentMusicLibraryBinding
import puelloc.musicplayer.entity.Playlist
import puelloc.musicplayer.viewmodel.MainActivityViewModel
import puelloc.musicplayer.viewmodel.MainActivityViewModel.Companion.SHOW_MUSIC_LIBRARY


class MusicLibraryFragment : Fragment() {
    private var _binding: FragmentMusicLibraryBinding? = null
    private var binding: FragmentMusicLibraryBinding
        get() = _binding!!
        set(value) {
            _binding = value
        }

    private val mainActivityViewModel: MainActivityViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentMusicLibraryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        mainActivityViewModel.showPlaylistId.observe(viewLifecycleOwner) {
            when (it) {
                SHOW_MUSIC_LIBRARY -> if (childFragmentManager.backStackEntryCount > 0) {
                    childFragmentManager.popBackStack()
                } else {
                    childFragmentManager.commit {
                        setReorderingAllowed(true)
                        replace<PlaylistFragment>(binding.fragmentContainerView.id)
                    }
                }
                else -> childFragmentManager.commit {
                    setReorderingAllowed(true)
                    replace<SongFragment>(
                        binding.fragmentContainerView.id,
                        args = bundleOf(SongFragment.PLAYLIST_ID_BUNDLE_KEY to it)
                    )
                    addToBackStack(null)
                }
            }
        }
    }
}