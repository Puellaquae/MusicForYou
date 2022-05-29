package puelloc.musicplayer.pojo.relation

import androidx.room.Embedded
import androidx.room.Junction
import androidx.room.Relation
import puelloc.musicplayer.entity.Playlist
import puelloc.musicplayer.entity.PlaylistSongCrossRef
import puelloc.musicplayer.entity.Song
import puelloc.musicplayer.trait.Equatable

data class PlaylistWithSongs(
    @Embedded val playlist: Playlist,
    @Relation(
        parentColumn = "playlistId",
        entityColumn = "songId",
        associateBy = Junction(PlaylistSongCrossRef::class)
    )
    val songs: List<Song>
) : Equatable