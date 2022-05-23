package puelloc.musicplayer.entity

import androidx.room.Embedded
import androidx.room.Relation
import puelloc.musicplayer.trait.Equatable

data class PlaybackQueueItemWithSong(
    @Embedded
    val queueItem: PlaybackQueueItem,
    @Relation(
        parentColumn = "songId",
        entityColumn = "songId"
    )
    val song: Song
) : Equatable
