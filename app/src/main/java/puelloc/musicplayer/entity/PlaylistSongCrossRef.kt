package puelloc.musicplayer.entity

import androidx.room.Entity
import puelloc.musicplayer.trait.Equatable

@Entity(primaryKeys = ["playlistId", "songId"])
data class PlaylistSongCrossRef(
    val playlistId: Long,
    val songId: Long
) : Equatable