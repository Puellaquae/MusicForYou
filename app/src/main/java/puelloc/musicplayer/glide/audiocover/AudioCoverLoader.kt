package puelloc.musicplayer.glide.audiocover

import com.bumptech.glide.load.Options
import com.bumptech.glide.load.model.ModelLoader
import com.bumptech.glide.load.model.ModelLoaderFactory
import com.bumptech.glide.load.model.MultiModelLoaderFactory
import com.bumptech.glide.signature.ObjectKey
import java.io.InputStream

class AudioCoverLoader : ModelLoader<AudioCover, InputStream> {
    override fun buildLoadData(
        audioCover: AudioCover,
        width: Int,
        height: Int,
        options: Options
    ): ModelLoader.LoadData<InputStream> {
        return ModelLoader.LoadData(
            ObjectKey(audioCover.filePath),
            AudioCoverFetcher(audioCover)
        )
    }

    override fun handles(audioFileCover: AudioCover): Boolean {
        return true
    }

    class Factory : ModelLoaderFactory<AudioCover, InputStream> {
        override fun build(multiFactory: MultiModelLoaderFactory): ModelLoader<AudioCover, InputStream> {
            return AudioCoverLoader()
        }

        override fun teardown() {}
    }
}