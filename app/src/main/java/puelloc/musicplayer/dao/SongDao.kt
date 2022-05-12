package puelloc.musicplayer.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import puelloc.musicplayer.entity.Song

@Dao
interface SongDao {
    @Query("SELECT * FROM Song")
    fun getAllSongs(): Flow<List<Song>>

    @Query("SELECT * FROM Song")
    fun getAllSongsSync(): List<Song>

    @Query("DELETE FROM Song")
    fun clearAll()

    @Insert
    fun insert(songs: List<Song>)
}