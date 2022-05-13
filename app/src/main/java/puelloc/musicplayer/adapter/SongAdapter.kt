package puelloc.musicplayer.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
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
        fun bind(song: Song) {
            itemBind.apply {
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
        holder.bind(song)
    }
}