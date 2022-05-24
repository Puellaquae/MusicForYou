package puelloc.musicplayer.glide.audiocover

import android.media.MediaMetadataRetriever
import com.bumptech.glide.Priority
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.data.DataFetcher
import java.io.ByteArrayInputStream
import java.io.FileNotFoundException
import java.io.IOException
import java.io.InputStream

class AudioCoverFetcher(private val model: AudioCover) : DataFetcher<InputStream> {
    private var stream: InputStream? = null
    override fun loadData(priority: Priority, callback: DataFetcher.DataCallback<in InputStream>) {
        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(model.filePath)
            val picture = retriever.embeddedPicture
            if (picture != null) {
                stream = ByteArrayInputStream(picture)
                callback.onDataReady(stream)
            } else {
                callback.onDataReady(null)
            }
        } catch (e: FileNotFoundException) {
            callback.onLoadFailed(e)
        } finally {
            retriever.release()
        }
    }

    override fun cleanup() {
        // already cleaned up in loadData and ByteArrayInputStream will be GC'd
        if (stream != null) {
            try {
                stream?.close()
            } catch (ignore: IOException) {
                // can't do much about it
            }
        }
    }

    override fun cancel() {
        // cannot cancel
    }

    override fun getDataClass(): Class<InputStream> {
        return InputStream::class.java
    }

    override fun getDataSource(): DataSource {
        return DataSource.LOCAL
    }
}