package puelloc.musicplayer.ui.fragment

import android.app.Application
import android.os.Bundle
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SwitchPreference
import puelloc.musicplayer.R
import puelloc.musicplayer.viewmodel.PlaybackQueueViewModel

class SettingFragment : PreferenceFragmentCompat() {
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences, rootKey)
        val playbackQueueViewModel =
            PlaybackQueueViewModel.getInstance(requireContext().applicationContext as Application)
        val playOncePreference: SwitchPreference? = findPreference("play_queue_once")
        playbackQueueViewModel.playOnceSongName.observe(this) { name ->
            playOncePreference?.summaryProvider = Preference.SummaryProvider<SwitchPreference> {
                if (it.isChecked) {
                    if (name != null) {
                        this.getString(R.string.play_queue_once_summary_on, name)
                    } else {
                        this.getString(R.string.play_queue_once_summary_on_wait)
                    }
                } else {
                    this.getString(R.string.play_queue_once_summary_off)
                }
            }
        }
    }
}