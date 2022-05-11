package puelloc.musicplayer.entity

import android.graphics.Bitmap

data class Song(
    val id: String,
    val name: String,
    val albumName: String,
    val artistName: String,
    val path: String,
    val duration: Long,
    var cover: Bitmap? = null
)
