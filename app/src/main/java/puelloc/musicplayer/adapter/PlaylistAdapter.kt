package puelloc.musicplayer.adapter

import android.content.Intent
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import puelloc.musicplayer.PLAYLIST_ID_MESSAGE
import puelloc.musicplayer.R
import puelloc.musicplayer.databinding.ItemPlaylistBinding
import puelloc.musicplayer.entity.Playlist
import puelloc.musicplayer.entity.PlaylistWithSongs
import puelloc.musicplayer.glide.audiocover.AudioCover
import puelloc.musicplayer.ui.activity.PlaylistActivity

class PlaylistAdapter :
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

    class ViewHolder(private val itemBinding: ItemPlaylistBinding) :
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
                    val intent = Intent(it.context, PlaylistActivity::class.java).apply {
                        putExtra(PLAYLIST_ID_MESSAGE, playlistWithSongs.playlist.playlistId)
                    }
                    it.context.startActivity(intent)
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
            )
        )
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val playlistWithSongs = getItem(position)
        holder.bind(playlistWithSongs)
    }
}