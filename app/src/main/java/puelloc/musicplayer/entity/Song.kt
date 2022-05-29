package puelloc.musicplayer.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import org.jetbrains.annotations.NotNull
import puelloc.musicplayer.trait.Equatable

@Entity
data class Song(
    @NotNull
    @PrimaryKey val songId: Long,
    val name: String,
    val albumName: String,
    val artistName: String,
    val path: String,
    val duration: Long,
) : Equatable
