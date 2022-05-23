package puelloc.musicplayer.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow
import puelloc.musicplayer.entity.PlaybackQueueItem
import puelloc.musicplayer.entity.PlaybackQueueItemWithSong

@Dao
abstract class PlaybackQueueDao {
    @Transaction
    @Query("SELECT * FROM PlaybackQueue ORDER BY `order`")
    abstract fun getPlaybackQueue(): Flow<List<PlaybackQueueItemWithSong>>

    @Query("SELECT * FROM PlaybackQueue ORDER BY `order` DESC LIMIT 1")
    abstract fun lastItemSync(): PlaybackQueueItem?

    @Query("SELECT * FROM PlaybackQueue ORDER BY `order` LIMIT 1")
    abstract fun firstItemSync(): PlaybackQueueItem?

    @Query("SELECT * FROM PlaybackQueue WHERE itemId == :playbackQueueItemId")
    abstract fun getItemSync(playbackQueueItemId: Long): PlaybackQueueItem?

    @Transaction
    @Query("SELECT * FROM PlaybackQueue ORDER BY `order` DESC LIMIT 1")
    abstract fun lastSongSync(): PlaybackQueueItemWithSong?

    @Transaction
    @Query("SELECT * FROM PlaybackQueue ORDER BY `order` LIMIT 1")
    abstract fun firstSongSync(): PlaybackQueueItemWithSong?

    @Transaction
    @Query("SELECT * FROM PlaybackQueue WHERE itemId == :playbackQueueItemId")
    abstract fun getSongSync(playbackQueueItemId: Long): PlaybackQueueItemWithSong?

    @Query("SELECT * FROM PlaybackQueue ORDER BY `order` DESC LIMIT 1")
    abstract fun lastItem(): Flow<PlaybackQueueItem?>

    @Query("SELECT * FROM PlaybackQueue ORDER BY `order` LIMIT 1")
    abstract fun firstItem(): Flow<PlaybackQueueItem?>

    @Query("SELECT * FROM PlaybackQueue WHERE itemId == :playbackQueueItemId")
    abstract fun getItem(playbackQueueItemId: Long): Flow<PlaybackQueueItem?>

    @Transaction
    @Query("SELECT * FROM PlaybackQueue ORDER BY `order` DESC LIMIT 1")
    abstract fun lastSong(): Flow<PlaybackQueueItemWithSong?>

    @Transaction
    @Query("SELECT * FROM PlaybackQueue ORDER BY `order` LIMIT 1")
    abstract fun firstSong(): Flow<PlaybackQueueItemWithSong?>

    @Transaction
    @Query("SELECT * FROM PlaybackQueue WHERE itemId == :playbackQueueItemId")
    abstract fun getSong(playbackQueueItemId: Long): Flow<PlaybackQueueItemWithSong?>

    @Query("SELECT * FROM PlaybackQueue WHERE `order` > :order ORDER BY `order` LIMIT 1")
    abstract fun nextItemByOrder(order: Long): PlaybackQueueItem?

    @Query("SELECT * FROM PlaybackQueue WHERE `order` < :order ORDER BY `order` DESC LIMIT 1")
    abstract fun previousItemByOrder(order: Long): PlaybackQueueItem?

    @Insert
    abstract fun insert(playbackQueueItem: PlaybackQueueItem)

    @Transaction
    open fun append(songIds: List<Long>) {
        val lastOrder = lastItemSync()?.order?.plus(PlaybackQueueItem.ORDER_STEP) ?: 0L
        songIds.forEachIndexed { index, id ->
            insert(PlaybackQueueItem(songId = id, order = lastOrder + PlaybackQueueItem.ORDER_STEP * index))
        }
    }

    @Query("DELETE FROM PlaybackQueue")
    abstract fun clearQueue()
}