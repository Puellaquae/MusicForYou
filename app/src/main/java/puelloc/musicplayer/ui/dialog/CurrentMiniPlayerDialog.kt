package puelloc.musicplayer.ui.dialog

import android.app.Application
import android.app.Dialog
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMetadataRetriever
import android.media.MediaPlayer
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.view.View
import android.view.Window
import android.widget.SeekBar
import androidx.fragment.app.DialogFragment
import com.bumptech.glide.Glide
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import puelloc.musicplayer.R
import puelloc.musicplayer.databinding.FragmentMiniPlayerBinding
import puelloc.musicplayer.entity.Song
import puelloc.musicplayer.glide.audiocover.AudioCover
import puelloc.musicplayer.viewmodel.PlaybackQueueViewModel
import java.io.IOException
import java.util.*

class CurrentMiniPlayerDialog : DialogFragment() {
    private lateinit var binding: FragmentMiniPlayerBinding

    private lateinit var playbackQueueViewModel: PlaybackQueueViewModel

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        binding = FragmentMiniPlayerBinding.inflate(requireActivity().layoutInflater)
        playbackQueueViewModel =
            PlaybackQueueViewModel.getInstance(requireContext().applicationContext as Application)
        playbackQueueViewModel.currentSong.observe(this) { song ->
            if (song == null) {
                return@observe
            }
            binding.apply {
                itemTitle.text = song.name
                itemSubtitle.text = song.albumName
                itemTitle.isSelected = true
                itemSubtitle.isSelected = true

                val aSec = (song.duration / 1000) % 60
                val aMin = (song.duration / 1000) / 60
                timeAll.text = getString(R.string.song_time, aMin, aSec)
                seekBar.max = song.duration.toInt()

                songInfo.text = try {
                    val mediaExtractor = MediaExtractor()
                    mediaExtractor.setDataSource(song.path)
                    val format = mediaExtractor.getTrackFormat(0)
                    val bitRate = if (format.containsKey(MediaFormat.KEY_BIT_RATE)) {
                        format.getInteger(MediaFormat.KEY_BIT_RATE)
                    } else {
                        val m = MediaMetadataRetriever()
                        m.setDataSource(song.path)
                        Integer.valueOf(
                            m.extractMetadata(MediaMetadataRetriever.METADATA_KEY_BITRATE) ?: "0"
                        )
                    }
                    val sampleRate = format.getInteger(MediaFormat.KEY_SAMPLE_RATE)
                    "${bitRate / 1000} kbps • ${sampleRate.toDouble() / 1000.0} kHz"
                } catch (e: IOException) {
                    ""
                }

                Glide
                    .with(root)
                    .asDrawable()
                    .load(AudioCover(song.path))
                    .placeholder(R.drawable.ic_baseline_music_note_24)
                    .fallback(R.drawable.ic_baseline_music_note_24)
                    .into(itemImage)
            }
        }

        var inSeeking = false

        binding.apply {
            playPause.setOnClickListener {
                if (playbackQueueViewModel.playing.value == true) {
                    requireActivity().mediaController.transportControls.pause()
                } else {
                    requireActivity().mediaController.transportControls.play()
                }
            }

            var seekBarProcess = 0


            seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(
                    seekbar: SeekBar?,
                    progress: Int,
                    fromUser: Boolean
                ) {
                    seekBarProcess = progress
                    val sec = (progress / 1000) % 60
                    val min = (progress / 1000) / 60
                    timeNow.text = getString(R.string.song_time, min, sec)
                }

                override fun onStartTrackingTouch(seekbar: SeekBar?) {
                    inSeeking = true
                }

                override fun onStopTrackingTouch(seekbar: SeekBar?) {
                    inSeeking = false
                    requireActivity().mediaController.transportControls.seekTo(seekBarProcess.toLong())
                }
            })
        }

        playbackQueueViewModel.playing.observe(this) {
            binding.playPause.setBackgroundResource(
                if (it) {
                    R.drawable.ic_baseline_pause_24
                } else {
                    R.drawable.ic_outline_play_arrow_24
                }
            )
        }

        playbackQueueViewModel.currentPosition.observe(this) {
            if (!inSeeking) {
                binding.seekBar.setProgress(it, true)
            }
        }

        return activity?.let { activity ->
            val dialog = MaterialAlertDialogBuilder(activity)
                .setView(binding.root)
                .create().apply {
                    requestWindowFeature(Window.FEATURE_NO_TITLE)
                }
            dialog
        } ?: throw IllegalStateException("Activity cannot be null")
    }
}