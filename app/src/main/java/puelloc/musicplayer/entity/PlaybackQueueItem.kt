package puelloc.musicplayer.entity

import androidx.room.Entity

@Entity(tableName = "PlaybackQueue")
data class PlaybackQueueItem(
    val songId: Long,
    val order: Long
)
