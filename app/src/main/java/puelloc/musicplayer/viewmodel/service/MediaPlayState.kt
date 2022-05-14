package puelloc.musicplayer.viewmodel.service

import puelloc.musicplayer.entity.Song

class MediaPlayState private constructor() {
    companion object {
        private var INSTANCE: MediaPlayState? = null

        @Synchronized
        fun getInstance(): MediaPlayState {
            if (INSTANCE == null) {
                INSTANCE = MediaPlayState()
            }
            return INSTANCE!!
        }
    }

    private val songListeners: MutableList<(song: Song) -> Unit> = ArrayList()

    var song: Song? = null
        set(value) {
            value?.let {
                songListeners.forEach { it(value) }
                field = value
            }
        }

    fun registerSongListener(listener: (song: Song) -> Unit) {
        songListeners.add(listener)
    }

    fun unregisterSongListener(listener: (song: Song) -> Unit) {
        songListeners.remove(listener)
    }
}