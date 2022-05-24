package puelloc.musicplayer.entity

import androidx.room.*
import org.jetbrains.annotations.NotNull
import puelloc.musicplayer.trait.Equatable

@Entity(indices = [Index("playlistId")])
data class Playlist(
    @NotNull
    @PrimaryKey(autoGenerate = true) val playlistId: Long? = null,
    val name: String,
    val isFromFolder: Boolean = false
) : Equatable