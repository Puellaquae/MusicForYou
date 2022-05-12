package puelloc.musicplayer.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import org.jetbrains.annotations.NotNull

@Entity(indices = [Index("songId")])
data class Song(
    @NotNull
    @PrimaryKey val songId: Long,
    val name: String,
    val albumName: String,
    val artistName: String,
    val path: String,
    val duration: Long,
)
