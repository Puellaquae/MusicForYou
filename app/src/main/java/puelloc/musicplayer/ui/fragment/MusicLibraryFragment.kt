package puelloc.musicplayer.ui.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.commit
import androidx.fragment.app.replace
import puelloc.musicplayer.databinding.FragmentMusicLibraryBinding
import puelloc.musicplayer.trait.IHandleBackPress
import puelloc.musicplayer.trait.IHandleFAB
import puelloc.musicplayer.trait.IHandleMenuItemClick
import puelloc.musicplayer.viewmodel.MainActivityViewModel
import puelloc.musicplayer.viewmodel.MainActivityViewModel.Companion.MUSIC_LIBRARY_SHOW_PLAYLISTS

class MusicLibraryFragment : Fragment(), IHandleBackPress, IHandleMenuItemClick, IHandleFAB {
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
                MUSIC_LIBRARY_SHOW_PLAYLISTS -> if (childFragmentManager.backStackEntryCount > 0) {
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

    override fun onBackPressed(): Boolean {
        val currentFragment = getCurrentFragment()
        if (currentFragment is IHandleBackPress &&
            currentFragment.onBackPressed()
        ) {
            return true
        }
        return if (mainActivityViewModel.showPlaylistId.value != MUSIC_LIBRARY_SHOW_PLAYLISTS) {
            mainActivityViewModel.setToShowPlaylist(MUSIC_LIBRARY_SHOW_PLAYLISTS)
            true
        } else {
            false
        }
    }

    override fun onMenuItemClicked(menuItem: MenuItem): Boolean {
        val currentFragment = getCurrentFragment()
        if (currentFragment is IHandleMenuItemClick &&
            currentFragment.onMenuItemClicked(menuItem)
        ) {
            return true
        }
        return false
    }

    private fun getCurrentFragment() =
        childFragmentManager.findFragmentById(binding.fragmentContainerView.id)

    override fun onFABClick(): Boolean {
        val currentFragment = getCurrentFragment()
        if (currentFragment is IHandleFAB &&
            currentFragment.onFABClick()
        ) {
            return true
        }
        return false
    }
}