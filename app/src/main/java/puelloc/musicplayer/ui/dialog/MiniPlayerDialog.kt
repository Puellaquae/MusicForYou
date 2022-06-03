package puelloc.musicplayer.ui.dialog

import android.app.Dialog
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMetadataRetriever
import android.media.MediaPlayer
import android.os.Bundle
import android.text.TextUtils
import android.view.Window
import android.widget.SeekBar
import androidx.fragment.app.DialogFragment
import com.bumptech.glide.Glide
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import puelloc.musicplayer.R
import puelloc.musicplayer.databinding.FragmentMiniPlayerBinding
import puelloc.musicplayer.entity.Song
import puelloc.musicplayer.glide.audiocover.AudioCover
import java.io.IOException
import java.util.*

class MiniPlayerDialog(private val song: Song) : DialogFragment() {
    private lateinit var binding: FragmentMiniPlayerBinding

    private var playing = true
    private lateinit var mediaPlayer: MediaPlayer
    private lateinit var timer: Timer

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        binding = FragmentMiniPlayerBinding.inflate(requireActivity().layoutInflater)

        mediaPlayer = MediaPlayer()
        mediaPlayer.setDataSource(song.path)
        mediaPlayer.prepare()
        mediaPlayer.start()
        mediaPlayer.duration
        timer = Timer()
        timer.scheduleAtFixedRate(object : TimerTask() {
            override fun run() {
                binding.seekBar.post {
                    binding.seekBar.setProgress(mediaPlayer.currentPosition, true)
                }
            }
        }, 0, 1000)
        binding.apply {
            itemTitle.text = song.name
            itemSubtitle.text = song.albumName
            itemTitle.isSelected = true
            itemSubtitle.isSelected = true

            playPause.setOnClickListener {
                if (playing) {
                    playing = false
                    playPause.setBackgroundResource(R.drawable.ic_outline_play_arrow_24)
                    mediaPlayer.pause()
                    itemTitle.ellipsize = TextUtils.TruncateAt.MARQUEE
                } else {
                    playing = true
                    playPause.setBackgroundResource(R.drawable.ic_baseline_pause_24)
                    mediaPlayer.start()
                    itemTitle.ellipsize = TextUtils.TruncateAt.END
                }
            }
            val aSec = (song.duration / 1000) % 60
            val aMin = (song.duration / 1000) / 60
            timeAll.text = "$aMin:${String.format("%02d", aSec)}"
            seekBar.max = song.duration.toInt()

            val mediaExtractor = MediaExtractor()
            songInfo.text = try {
                mediaExtractor.setDataSource(song.path)
                val format = mediaExtractor.getTrackFormat(0)
                val bitRate = format.getInteger(MediaFormat.KEY_BIT_RATE)
                val sampleRate = format.getInteger(MediaFormat.KEY_SAMPLE_RATE)
                "${bitRate / 1000}KBPS • ${sampleRate.toDouble() / 1000.0}KHz"
            } catch (e: IOException) {
                ""
            }

            seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(
                    seekbar: SeekBar?,
                    progress: Int,
                    fromUser: Boolean
                ) {
                    if (fromUser) {
                        mediaPlayer.seekTo(progress)
                    }
                    val sec = (progress / 1000) % 60
                    val min = (progress / 1000) / 60
                    timeNow.text = "$min:${String.format("%02d", sec)}"
                }

                override fun onStartTrackingTouch(seekbar: SeekBar?) {

                }

                override fun onStopTrackingTouch(seekbar: SeekBar?) {

                }
            })

            Glide
                .with(root)
                .asDrawable()
                .load(AudioCover(song.path))
                .placeholder(R.drawable.ic_baseline_music_note_24)
                .fallback(R.drawable.ic_baseline_music_note_24)
                .into(itemImage)
        }

        return activity?.let { activity ->
            val dialog = MaterialAlertDialogBuilder(activity)
                .setView(binding.root)
                .create().apply {
                    requestWindowFeature(Window.FEATURE_NO_TITLE);
                }
            dialog
        } ?: throw IllegalStateException("Activity cannot be null")
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaPlayer.stop()
        mediaPlayer.release()
        timer.cancel()
        timer.purge()
    }
}