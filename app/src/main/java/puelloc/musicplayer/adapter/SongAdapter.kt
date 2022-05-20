package puelloc.musicplayer.adapter

import android.graphics.Color
import android.os.Build
import android.util.TypedValue
import android.view.ContextThemeWrapper
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.selection.SelectionTracker
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.android.material.color.DynamicColors
import com.google.android.material.color.MaterialColors
import kotlinx.coroutines.*
import puelloc.musicplayer.R
import puelloc.musicplayer.databinding.ItemSongBinding
import puelloc.musicplayer.entity.Song
import puelloc.musicplayer.glide.audiocover.AudioCover

class SongAdapter(private val onClick: (song: Song) -> Unit) :
    ListAdapter<Song, SongAdapter.ViewHolder>(object : DiffUtil.ItemCallback<Song>() {
        override fun areItemsTheSame(oldItem: Song, newItem: Song): Boolean =
            oldItem.songId == newItem.songId

        override fun areContentsTheSame(oldItem: Song, newItem: Song): Boolean =
            oldItem.hashCode() == newItem.hashCode()
    }) {
    class ViewHolder(
        private val itemBind: ItemSongBinding,
        private val onClick: (song: Song) -> Unit
    ) :
        RecyclerView.ViewHolder(itemBind.root) {
        fun bind(song: Song, selected: Boolean = false) {
            itemBind.apply {
                if (selected) {
                    root.background.setTint(
                        ContextCompat.getColor(root.context, R.color.selected)
                    )
                } else {
                    root.background.setTint(Color.TRANSPARENT)
                }
                songName.text = song.name
                songArtist.text = song.artistName
                Glide
                    .with(root)
                    .load(AudioCover(song.path))
                    .placeholder(R.drawable.ic_round_music_note_24)
                    .fallback(R.drawable.ic_round_music_note_24)
                    .into(songCover)
                root.setOnClickListener { onClick(song) }
            }
        }
    }

    var selectionTracker: SelectionTracker<Long>? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(
            ItemSongBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            ),
            onClick
        )
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val song = getItem(position)
        holder.bind(song, selectionTracker?.isSelected(song.songId) ?: false)
    }

    override fun getItemId(position: Int): Long {
        return getItem(position).songId
    }
}