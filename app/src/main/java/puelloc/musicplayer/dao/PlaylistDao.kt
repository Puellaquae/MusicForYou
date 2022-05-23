package puelloc.musicplayer.dao

import android.util.Log
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

    @Transaction
    @Query("SELECT * FROM playlist")
    abstract fun getAllPlaylistsWithSongsSync() : List<PlaylistWithSongs>

    @Transaction
    @Query("SELECT * FROM playlist WHERE isFromFolder == 0")
    abstract fun getAllNotFromFolderPlaylistsWithSongs() : Flow<List<PlaylistWithSongs>>

    @Insert
    abstract fun insert(playlist: Playlist): Long

    @Query("SELECT playlistId FROM playlist WHERE rowid == :rowId")
    abstract fun rowIdToPlaylistId(rowId: Long): Long

    @Insert(entity = PlaylistSongCrossRef::class, onConflict = OnConflictStrategy.REPLACE)
    abstract fun insertOrUpdateToRefPlaylistAndSong(playlistSongCrossRef: PlaylistSongCrossRef)

    @Query("SELECT playlistId FROM Playlist WHERE name == :name AND isFromFolder == 1")
    abstract fun getOldFolderPlaylistId(name: String): Long

    @Transaction
    open fun insertOrUpdatePlaylistsWithSons(playlistsWithSongs: List<PlaylistWithSongs>) {
        playlistsWithSongs.forEach { playlistWithSongs ->
            val rowId = insert(playlistWithSongs.playlist)
            val playlistId = rowIdToPlaylistId(rowId)
            playlistWithSongs.songs.forEach { song ->
                insertOrUpdateToRefPlaylistAndSong(PlaylistSongCrossRef(playlistId, song.songId))
            }
        }
    }

    @Transaction
    open fun insertOrUpdatePlaylistsWithSonsFromFolderStable(playlistsWithSongs: List<PlaylistWithSongs>) {
        playlistsWithSongs.forEach { playlistWithSongs ->
            var playlistId = getOldFolderPlaylistId(playlistWithSongs.playlist.name)
            if (playlistId == 0L) {
                val rowId = insert(playlistWithSongs.playlist)
                playlistId = rowIdToPlaylistId(rowId)
            }
            playlistWithSongs.songs.forEach { song ->
                insertOrUpdateToRefPlaylistAndSong(PlaylistSongCrossRef(playlistId, song.songId))
            }
        }
    }

    open fun clearEmptyFolderPlaylist() {
        getAllPlaylistsWithSongsSync().filter { it.playlist.isFromFolder && it.songs.isEmpty() }.forEach {
            deleteByPlaylistId(it.playlist.playlistId!!)
        }
    }

    @Query("SELECT * FROM playlist WHERE isFromFolder == 1")
    abstract fun getPlaylistsWhichBuiltFromFolder(): List<Playlist>

    @Query("DELETE FROM playlistsongcrossref WHERE playlistId == :playlistId")
    abstract fun deleteToDeRefAllSongsOfPlaylist(playlistId: Long)

    @Query("DELETE FROM playlistsongcrossref WHERE playlistId == :playlistId AND songId == :songId")
    abstract fun removeSongFromPlaylist(playlistId: Long, songId: Long)

    @Delete
    abstract fun delete(playlist: Playlist)

    /**
     * This won't delete the link between playlist and song, so call [deleteToDeRefAllSongsOfPlaylist] first.
     */
    @Query("DELETE FROM playlist WHERE playlistId == :playlistId")
    abstract fun deleteByPlaylistId(playlistId: Long)

    @Transaction
    open fun clearPlaylistsWhichBuiltFromFolder() {
        getPlaylistsWhichBuiltFromFolder().forEach {
            deleteToDeRefAllSongsOfPlaylist(it.playlistId!!)
            delete(it)
        }
    }

    @Query("SELECT playlistId FROM playlist WHERE isFromFolder == 0 AND playlistId IN (:playlistIds)")
    abstract fun excludeFolderPlaylistIdSync(playlistIds: List<Long>): List<Long>

    @Query("DELETE FROM PlaylistSongCrossRef WHERE songId IN (:songIds)")
    abstract fun deleteSongs(songIds: List<Long>)
}