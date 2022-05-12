package puelloc.musicplayer.entity

import androidx.room.Entity
import androidx.room.Index

@Entity(primaryKeys = ["playlistId", "songId"], indices = [Index("playlistId"), Index("songId")])
data class PlaylistSongCrossRef(
    val playlistId: Long,
    val songId: Long
)