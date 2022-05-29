package puelloc.musicplayer.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import puelloc.musicplayer.entity.Playlist
import puelloc.musicplayer.entity.PlaylistSongCrossRef
import puelloc.musicplayer.pojo.relation.PlaylistWithSongs

@Dao
abstract class PlaylistDao {
    @Query("SELECT * FROM Playlist")
    abstract fun getAllPlaylist() : LiveData<List<Playlist>>

    @Query("SELECT * FROM PlaylistSongCrossRef")
    abstract fun getAllPlaylistSongCrossRefs() : LiveData<List<PlaylistSongCrossRef>>

    @Query("SELECT * FROM Playlist where playlistId == :playlistId")
    abstract fun getPlaylist(playlistId: Long) : LiveData<Playlist>

    @Transaction
    @Query("SELECT * FROM Playlist where playlistId == :playlistId")
    abstract fun getPlaylistWithSongs(playlistId: Long) : LiveData<PlaylistWithSongs>

    @Transaction
    @Query("SELECT * FROM Playlist")
    abstract fun getAllPlaylistsWithSongs() : LiveData<List<PlaylistWithSongs>>

    @Transaction
    @Query("SELECT * FROM Playlist")
    abstract fun getAllPlaylistsWithSongsSync() : List<PlaylistWithSongs>

    @Transaction
    @Query("SELECT * FROM Playlist WHERE isFromFolder == 0")
    abstract fun getAllNotFromFolderPlaylistsWithSongs() : LiveData<List<PlaylistWithSongs>>

    @Insert
    abstract fun insert(playlist: Playlist): Long

    @Query("SELECT playlistId FROM Playlist WHERE rowid == :rowId")
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

    @Query("SELECT * FROM Playlist WHERE isFromFolder == 1")
    abstract fun getPlaylistsWhichBuiltFromFolder(): List<Playlist>

    @Query("DELETE FROM PlaylistSongCrossRef WHERE playlistId == :playlistId")
    abstract fun deleteToDeRefAllSongsOfPlaylist(playlistId: Long)

    @Query("DELETE FROM PlaylistSongCrossRef WHERE playlistId == :playlistId AND songId == :songId")
    abstract fun removeSongFromPlaylist(playlistId: Long, songId: Long)

    @Delete
    abstract fun delete(playlist: Playlist)

    /**
     * This won't delete the link between playlist and song, so call [deleteToDeRefAllSongsOfPlaylist] first.
     */
    @Query("DELETE FROM Playlist WHERE playlistId == :playlistId")
    abstract fun deleteByPlaylistId(playlistId: Long)

    @Transaction
    open fun clearPlaylistsWhichBuiltFromFolder() {
        getPlaylistsWhichBuiltFromFolder().forEach {
            deleteToDeRefAllSongsOfPlaylist(it.playlistId!!)
            delete(it)
        }
    }

    @Query("SELECT playlistId FROM Playlist WHERE isFromFolder == 0 AND playlistId IN (:playlistIds)")
    abstract fun excludeFolderPlaylistIdSync(playlistIds: List<Long>): List<Long>

    @Query("DELETE FROM PlaylistSongCrossRef WHERE songId IN (:songIds)")
    abstract fun deleteSongs(songIds: List<Long>)
}