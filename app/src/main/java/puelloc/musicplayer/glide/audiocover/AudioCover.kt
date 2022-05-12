package puelloc.musicplayer.glide.audiocover

class AudioCover(val filePath: String) {
    override fun hashCode(): Int {
        return filePath.hashCode()
    }

    override fun equals(other: Any?): Boolean {
        return if (other is AudioCover) {
            other.filePath == filePath
        } else false
    }
}