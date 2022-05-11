package puelloc.musicplayer.adapter

import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.appcompat.content.res.AppCompatResources
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.findViewTreeLifecycleOwner
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import puelloc.musicplayer.R
import puelloc.musicplayer.databinding.ItemSongBinding
import puelloc.musicplayer.entity.Song
import puelloc.musicplayer.viewmodel.SongViewModel

class SongAdapter :
    ListAdapter<Song, SongAdapter.ViewHolder>(object : DiffUtil.ItemCallback<Song>() {
        override fun areItemsTheSame(oldItem: Song, newItem: Song): Boolean =
            oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: Song, newItem: Song): Boolean =
            oldItem.hashCode() == newItem.hashCode()
    }) {
    class ViewHolder(
        private val itemBind: ItemSongBinding
    ) :
        RecyclerView.ViewHolder(itemBind.root) {
        fun bind(song: Song) {
            itemBind.apply {
                songName.text = song.name
                songArtist.text = song.artistName
                songCover.setImageDrawable(
                    AppCompatResources.getDrawable(
                        itemBind.root.context,
                        R.drawable.ic_round_music_note_24
                    )
                )
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
        val song = getItem(position)
        holder.bind(song)
    }
}