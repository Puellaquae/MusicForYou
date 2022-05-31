package puelloc.musicplayer.utils

import android.content.Context
import android.support.v4.media.MediaMetadataCompat
import com.bumptech.glide.Glide
import puelloc.musicplayer.R
import puelloc.musicplayer.entity.Song
import puelloc.musicplayer.glide.audiocover.AudioCover

class SongUtil {
    companion object {
        fun Song.getMetadataBuilder() =
            MediaMetadataCompat.Builder().apply {
                putString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID, songId.toString())
                putString(MediaMetadataCompat.METADATA_KEY_TITLE, name)
                putString(MediaMetadataCompat.METADATA_KEY_ARTIST, artistName)
                putString(MediaMetadataCompat.METADATA_KEY_ALBUM, albumName)
                putLong(MediaMetadataCompat.METADATA_KEY_DURATION, duration)
                putString(MediaMetadataCompat.METADATA_KEY_DISPLAY_TITLE, name)
                putString(MediaMetadataCompat.METADATA_KEY_DISPLAY_SUBTITLE, artistName)
            }
    }
}