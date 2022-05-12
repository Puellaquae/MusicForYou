package puelloc.musicplayer.dao

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import puelloc.musicplayer.entity.Playlist
import puelloc.musicplayer.entity.PlaylistSongCrossRef
import puelloc.musicplayer.entity.PlaylistWithSongs

@Dao
abstract class PlaylistDao {
    @Query("SELECT * FROM playlist")
    abstract fun getAllPlaylist() : Flow<List<Playlist>>

    @Query("SELECT * FROM playlist where playlistId == :playlistId")
    abstract fun getPlaylist(playlistId: Long) : Flow<Playlist>

    @Transaction
    @Query("SELECT * FROM playlist where playlistId == :playlistId")
    abstract fun getPlaylistWithSongs(playlistId: Long) : Flow<PlaylistWithSongs>

    @Transaction
    @Query("SELECT * FROM playlist")
    abstract fun getAllPlaylistsWithSongs() : Flow<List<PlaylistWithSongs>>

    @Insert
    abstract fun insert(playlist: Playlist): Long

    @Query("SELECT playlistId FROM playlist WHERE rowid == :rowId")
    abstract fun rowIdToPlaylistId(rowId: Long): Long

    @Insert(entity = PlaylistSongCrossRef::class, onConflict = OnConflictStrategy.REPLACE)
    abstract fun insertToRefPlaylistAndSong(playlistSongCrossRef: PlaylistSongCrossRef)

    @Transaction
    open fun insertPlaylistsWithSons(playlistsWithSongs: List<PlaylistWithSongs>) {
        playlistsWithSongs.forEach { playlistWithSongs ->
            val rowId = insert(playlistWithSongs.playlist)
            val playlistId = rowIdToPlaylistId(rowId)
            playlistWithSongs.songs.forEach { song ->
                insertToRefPlaylistAndSong(PlaylistSongCrossRef(playlistId, song.songId))
            }
        }
    }

    @Query("SELECT * FROM playlist WHERE isFromFolder == 1")
    abstract fun getPlaylistsWhichBuiltFromFolder(): List<Playlist>

    @Query("DELETE FROM playlistsongcrossref WHERE playlistId == :playlistId")
    abstract fun deleteToDeRefAllSongsOfPlaylist(playlistId: Long)

    @Delete
    abstract fun delete(playlist: Playlist)

    @Transaction
    open fun clearPlaylistsWhichBuiltFromFolder() {
        getPlaylistsWhichBuiltFromFolder().forEach {
            deleteToDeRefAllSongsOfPlaylist(it.playlistId!!)
            delete(it)
        }
    }
}