package puelloc.musicplayer.dao

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import puelloc.musicplayer.entity.Song

@Dao
interface SongDao {
    @Query("SELECT * FROM Song")
    fun getAllSongs(): Flow<List<Song>>

    @Query("SELECT * FROM Song")
    fun getAllSongsSync(): List<Song>

    @Query("SELECT * FROM Song WHERE songId == :songId")
    fun getSong(songId: Long): Flow<Song>

    @Query("DELETE FROM Song")
    fun clearAll()

    @Insert
    fun insert(songs: List<Song>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertOrUpdate(songs: List<Song>)

    @Query("SELECT songId FROM Song WHERE songId NOT IN (:songIds)")
    fun selectNotExistedSongIdSync(songIds: List<Long>): List<Long>

    @Query("DELETE FROM Song WHERE songId IN (:songIds)")
    fun deleteSongs(songIds: List<Long>)
}