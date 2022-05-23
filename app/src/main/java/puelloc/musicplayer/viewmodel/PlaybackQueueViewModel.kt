package puelloc.musicplayer.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import puelloc.musicplayer.db.AppDatabase

class PlaybackQueueViewModel(application: Application) : AndroidViewModel(application) {
    companion object {
        private var INSTANCE: PlaybackQueueViewModel? = null

        @Synchronized
        fun getInstance(application: Application): PlaybackQueueViewModel {
            if (INSTANCE == null) {
                INSTANCE = PlaybackQueueViewModel(application)
            }
            return INSTANCE!!
        }

        private val TAG = this::class.java.declaringClass.simpleName
    }

    private val appDatabase =
        AppDatabase.getDatabase(getApplication<Application?>().applicationContext)
    private val playbackQueueDao = appDatabase.playbackQueueDao()

    val playbackQueueWithSong = playbackQueueDao.getPlaybackQueue().asLiveData()

    fun appendSongs(songIds: List<Long>) {
        viewModelScope.launch(Dispatchers.IO) {
            playbackQueueDao.append(songIds)
        }
    }

    fun nextSong() {
        viewModelScope.launch(Dispatchers.IO) {
            // The initial state, try play the first song
            if (currentItemId.value == 0L) {
                firstSong()
            } else {
                // get current item
                val currentItem = playbackQueueDao.getItemSync(currentItemId.value!!)
                // current item is not existed in playback queue, try play first song
                if (currentItem == null) {
                    firstSong()
                } else {
                    val currentOrder = currentItem.order
                    val next = playbackQueueDao.nextItemByOrder(currentOrder)
                    Log.d(TAG, "curOrder: $currentOrder, nextOrder: ${next?.order}")
                    // no next song
                    if (next == null) {
                        firstSong()
                    } else {
                        currentItemId.postValue(next.itemId!!)
                    }
                }
            }
        }
    }

    private suspend fun firstSong() {
        withContext(Dispatchers.IO) {
            currentItemId.postValue(
                playbackQueueDao.firstSongSync()?.queueItem?.itemId ?: 0L
            )
        }
    }

    private suspend fun lastSong() {
        withContext(Dispatchers.IO) {
            currentItemId.postValue(
                playbackQueueDao.lastSongSync()?.queueItem?.itemId ?: 0L
            )
        }
    }

    fun previousSong() {
        viewModelScope.launch(Dispatchers.IO) {
            if (currentItemId.value == 0L) {
                lastSong()
            } else {
                val currentItem = playbackQueueDao.getItemSync(currentItemId.value!!)
                if (currentItem == null) {
                    lastSong()
                } else {
                    val currentOrder = currentItem.order
                    val prev = playbackQueueDao.previousItemByOrder(currentOrder)
                    Log.d(TAG, "curOrder: $currentOrder, prevOrder: ${prev?.order}")
                    if (prev == null) {
                        lastSong()
                    } else {
                        currentItemId.postValue(prev.itemId!!)
                    }
                }
            }
        }
    }

    fun clearQueue() {
        viewModelScope.launch(Dispatchers.IO) {
            playbackQueueDao.clearQueue()
        }
    }

    val currentItemId = MutableLiveData<Long>().apply {
        postValue(0L)
    }

    val currentSong = currentItemId.switchMap { id ->
        liveData {
            withContext(Dispatchers.IO) {
                val song = playbackQueueDao.getSongSync(id)
                emit(song?.song)
            }
        }
    }

    val playable = MutableLiveData<Boolean>()
}