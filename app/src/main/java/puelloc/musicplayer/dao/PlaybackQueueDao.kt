package puelloc.musicplayer.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import puelloc.musicplayer.entity.PlaybackQueueItem
import puelloc.musicplayer.pojo.relation.PlaybackQueueItemWithSong

@Dao
abstract class PlaybackQueueDao {
    @Transaction
    @Query("SELECT * FROM PlaybackQueue ORDER BY `order`")
    abstract fun getPlaybackQueue(): LiveData<List<PlaybackQueueItemWithSong>>

    @Transaction
    @Query("SELECT * FROM PlaybackQueue ORDER BY `order`")
    abstract fun getPlaybackQueueSync(): List<PlaybackQueueItemWithSong>

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
    abstract fun lastItem(): LiveData<PlaybackQueueItem?>

    @Query("SELECT * FROM PlaybackQueue ORDER BY `order` LIMIT 1")
    abstract fun firstItem(): LiveData<PlaybackQueueItem?>

    @Query("SELECT * FROM PlaybackQueue WHERE itemId == :playbackQueueItemId")
    abstract fun getItem(playbackQueueItemId: Long): LiveData<PlaybackQueueItem?>

    @Transaction
    @Query("SELECT * FROM PlaybackQueue ORDER BY `order` DESC LIMIT 1")
    abstract fun lastSong(): LiveData<PlaybackQueueItemWithSong?>

    @Transaction
    @Query("SELECT * FROM PlaybackQueue ORDER BY `order` LIMIT 1")
    abstract fun firstSong(): LiveData<PlaybackQueueItemWithSong?>

    @Transaction
    @Query("SELECT * FROM PlaybackQueue WHERE itemId == :playbackQueueItemId")
    abstract fun getSong(playbackQueueItemId: Long): LiveData<PlaybackQueueItemWithSong?>

    @Query("SELECT * FROM PlaybackQueue WHERE `order` > :order ORDER BY `order` LIMIT 1")
    abstract fun nextItemByOrder(order: Long): PlaybackQueueItem?

    @Query("SELECT * FROM PlaybackQueue WHERE `order` < :order ORDER BY `order` DESC LIMIT 1")
    abstract fun previousItemByOrder(order: Long): PlaybackQueueItem?

    @Insert
    abstract fun insert(playbackQueueItem: PlaybackQueueItem)

    open fun append(songIds: List<Long>) {
        val lastOrder = (lastItemSync()?.order ?: 0L) + PlaybackQueueItem.ORDER_STEP
        songIds.forEachIndexed { index, id ->
            insert(
                PlaybackQueueItem(
                    songId = id,
                    order = lastOrder + PlaybackQueueItem.ORDER_STEP * index
                )
            )
        }
    }

    @Query("DELETE FROM PlaybackQueue")
    abstract fun clearQueue()

    @Query("SELECT COUNT(*) FROM PlaybackQueue")
    abstract fun size(): Int

    /**
     * sub step for each [start, end]
     */
    @Query("UPDATE PlaybackQueue SET `order` = `order` - :step WHERE `order` >= :startOrder AND `order` <= :endOrder")
    abstract fun moveRangeUp(startOrder: Long, endOrder: Long, step: Long)

    /**
     * add step for each [start, end]
     */
    @Query("UPDATE PlaybackQueue SET `order` = `order` + :step WHERE `order` >= :startOrder AND `order` <= :endOrder")
    abstract fun moveRangeDown(startOrder: Long, endOrder: Long, step: Long)

    @Query("UPDATE PlaybackQueue SET `order` = :newOrder WHERE itemId == :itemId")
    abstract fun updateOrder(itemId: Long, newOrder: Long)
}