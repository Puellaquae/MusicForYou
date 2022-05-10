package puelloc.musicplayer.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import puelloc.musicplayer.databinding.ItemSongBinding
import puelloc.musicplayer.entity.Song

class SongAdapter :
    ListAdapter<Song, SongAdapter.ViewHolder>(object : DiffUtil.ItemCallback<Song>() {
        override fun areItemsTheSame(oldItem: Song, newItem: Song): Boolean =
            oldItem == newItem

        override fun areContentsTheSame(oldItem: Song, newItem: Song): Boolean =
            oldItem == newItem
    }) {
    class ViewHolder(private val itemBind: ItemSongBinding) :
        RecyclerView.ViewHolder(itemBind.root) {
            fun bind(song: Song) {
                itemBind.apply {
                    songName.text = song.name
                    songArtist.text = song.artistName
                }
            }
        }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(
            ItemSongBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
}