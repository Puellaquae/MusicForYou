package puelloc.musicplayer.pojo

import puelloc.musicplayer.entity.Song
import puelloc.musicplayer.trait.Equatable

data class PlaylistWithFirstSongAndSongCount(
    val playlistId: Long,
    val name: String,
    val isFromFolder: Boolean,
    val firstSong: Song?,
    val songCount: Int
) : Equatable
