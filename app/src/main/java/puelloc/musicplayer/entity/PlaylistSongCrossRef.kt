package puelloc.musicplayer.entity

import androidx.room.Entity
import androidx.room.Index
import puelloc.musicplayer.trait.Equatable

@Entity(primaryKeys = ["playlistId", "songId"], indices = [Index("songId"), Index("playlistId")])
data class PlaylistSongCrossRef(
    val playlistId: Long,
    val songId: Long
) : Equatable