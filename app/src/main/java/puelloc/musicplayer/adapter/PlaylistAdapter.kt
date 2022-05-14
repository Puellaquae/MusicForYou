package puelloc.musicplayer.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import puelloc.musicplayer.R
import puelloc.musicplayer.databinding.ItemPlaylistBinding
import puelloc.musicplayer.entity.PlaylistWithSongs
import puelloc.musicplayer.glide.audiocover.AudioCover

class PlaylistAdapter(private val onClick: (playlistWithSongs: PlaylistWithSongs) -> Unit) :
    ListAdapter<PlaylistWithSongs, PlaylistAdapter.ViewHolder>(object :
        DiffUtil.ItemCallback<PlaylistWithSongs>() {
        override fun areItemsTheSame(
            oldItem: PlaylistWithSongs,
            newItem: PlaylistWithSongs
        ): Boolean =
            oldItem.playlist.playlistId == newItem.playlist.playlistId

        override fun areContentsTheSame(
            oldItem: PlaylistWithSongs,
            newItem: PlaylistWithSongs
        ): Boolean =
            oldItem.hashCode() == newItem.hashCode()
    }) {

    class ViewHolder(
        private val itemBinding: ItemPlaylistBinding,
        private val onClick: (playlistWithSongs: PlaylistWithSongs) -> Unit
    ) :
        RecyclerView.ViewHolder(itemBinding.root) {
        fun bind(playlistWithSongs: PlaylistWithSongs) {
            itemBinding.apply {
                playlistName.text = playlistWithSongs.playlist.name
                playlistCount.text = "${playlistWithSongs.songs.size} songs"
                Glide
                    .with(root)
                    .load(AudioCover(playlistWithSongs.songs.first().path))
                    .placeholder(R.drawable.ic_baseline_music_note_24)
                    .fallback(R.drawable.ic_baseline_music_note_24)
                    .into(playlistCover)
                root.setOnClickListener {
                    onClick(playlistWithSongs)
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(
            ItemPlaylistBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            ),
            onClick
        )
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val playlistWithSongs = getItem(position)
        holder.bind(playlistWithSongs)
    }
}