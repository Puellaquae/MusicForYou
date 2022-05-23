package puelloc.musicplayer.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import org.jetbrains.annotations.NotNull

@Entity(tableName = "PlaybackQueue", indices = [Index("order")])
data class PlaybackQueueItem(
    @NotNull
    @PrimaryKey(autoGenerate = true) val itemId: Long? = null,
    val songId: Long,
    val order: Long
) {
    companion object {
        const val ORDER_STEP = 32768L
    }
}
