package puelloc.musicplayer.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import org.jetbrains.annotations.NotNull
import puelloc.musicplayer.trait.Equatable

@Entity(tableName = "PlaybackQueue")
data class PlaybackQueueItem(
    @NotNull
    @PrimaryKey(autoGenerate = true) val itemId: Long? = null,
    val songId: Long,
    val order: Long
) : Equatable {
    companion object {
        const val ORDER_STEP = 1L
    }
}
