package puelloc.musicplayer.viewmodel

import android.app.Application
import android.content.SharedPreferences
import android.util.Log
import androidx.lifecycle.*
import androidx.preference.PreferenceManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import puelloc.musicplayer.db.AppDatabase
import puelloc.musicplayer.entity.PlaybackQueueItem
import puelloc.musicplayer.entity.Playlist
import puelloc.musicplayer.entity.PlaylistSongCrossRef
import puelloc.musicplayer.enums.AppEvent
import puelloc.musicplayer.enums.PlaybackEvent
import puelloc.musicplayer.utils.SingleLiveEvent

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

        const val NONE_SONG_ITEM_ID = -1L
    }

    private val appDatabase =
        AppDatabase.getDatabase(getApplication<Application?>().applicationContext)
    private val playbackQueueDao = appDatabase.playbackQueueDao()
    private val playlistDao = appDatabase.playlistDao()

    val playbackQueueWithSong = playbackQueueDao.getPlaybackQueue()

    private val settings: SharedPreferences?
        get() {
            val context = getApplication<Application?>().applicationContext
            return PreferenceManager.getDefaultSharedPreferences(context)
        }

    fun appendSongs(songIds: List<Long>) {
        viewModelScope.launch(Dispatchers.IO) {
            needAutoSave.postValue(true)
            playbackQueueDao.append(songIds)
        }
    }

    private fun nextSong() {
        viewModelScope.launch(Dispatchers.IO) {
            // The initial state, try play the first song
            if (currentItemId.value == NONE_SONG_ITEM_ID) {
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
                        if (settings?.getBoolean("infinity_play_queue", true) == true) {
                            firstSong()
                        } else {
                            emit(PlaybackEvent.STOP)
                        }
                    } else {
                        if (settings?.getBoolean("play_queue_once", false) == true
                            && next.itemId!! == _beginSongItemId.value
                        ) {
                            emit(PlaybackEvent.STOP)
                        } else {
                            _currentItemId.postValue(next.itemId!!)
                        }
                    }
                }
            }
            //_event.postValue(PlaybackEvent.PREPARE_FOR_NEW_SONG)
        }
    }

    private suspend fun firstSong() {
        withContext(Dispatchers.IO) {
            _currentItemId.postValue(
                playbackQueueDao.firstSongSync()?.queueItem?.itemId ?: NONE_SONG_ITEM_ID
            )
        }
    }

    private suspend fun lastSong() {
        withContext(Dispatchers.IO) {
            _currentItemId.postValue(
                playbackQueueDao.lastSongSync()?.queueItem?.itemId ?: NONE_SONG_ITEM_ID
            )
        }
    }

    private fun previousSong() {
        viewModelScope.launch(Dispatchers.IO) {
            if (currentItemId.value == NONE_SONG_ITEM_ID) {
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
                        _currentItemId.postValue(prev.itemId!!)
                    }
                }
            }
            //_event.postValue(PlaybackEvent.PREPARE_FOR_NEW_SONG)
        }
    }

    fun clearQueue() {
        viewModelScope.launch(Dispatchers.IO) {
            playbackQueueDao.clearQueue()
            needAutoSave.postValue(false)
        }
    }

    fun saveToPlaylist(playlistId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            saveToPlaylistSync(playlistId)
        }
    }

    private fun saveToPlaylistSync(playlistId: Long) {
        val queue = playbackQueueDao.getPlaybackQueueSync()
        queue.forEach {
            playlistDao.insertOrUpdateToRefPlaylistAndSong(
                PlaylistSongCrossRef(
                    playlistId,
                    it.song.songId
                )
            )
        }
    }

    /**
     * will be changed in [deleteItemId], [moveItemAndInsert] and [appendSongs]
     * reset in [playPlaylist], [clearQueue]
     */
    private val needAutoSave = MutableLiveData<Boolean>()

    /**
     * Auto save current playback queue if not empty
     */
    fun playPlaylist(songIds: List<Long>, songId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            if (playbackQueueDao.size() != 0 && needAutoSave.value == true) {
                val playlistId =
                    playlistDao.rowIdToPlaylistId(playlistDao.insert(Playlist(name = "AutoSave")))
                saveToPlaylistSync(playlistId)
                playbackQueueDao.clearQueue()
            }
            playbackQueueDao.append(songIds)
            needAutoSave.postValue(false)
            emit(PlaybackEvent.CHOOSE_SONG_AND_PLAY, songId)
        }
    }

    private val _currentItemId = MutableLiveData<Long>().apply {
        postValue(NONE_SONG_ITEM_ID)
    }

    val currentItemId: LiveData<Long> = _currentItemId

    val currentSong = currentItemId.switchMap { id ->
        liveData {
            withContext(Dispatchers.IO) {
                val song = playbackQueueDao.getSongSync(id)
                if (song != null) {
                    emit(song.song)
                    if (waitBeginRecord) {
                        _beginSongItemId.postValue(id)
                    }
                    _event.postValue(PlaybackEvent.PREPARE_FOR_NEW_SONG)
                }
            }
        }
    }

    fun moveItemAndInsert(item: PlaybackQueueItem, positionOrder: Long, insertBefore: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            needAutoSave.postValue(true)
            if (insertBefore) {
                playbackQueueDao.moveRangeDown(positionOrder, item.order, 1)
                playbackQueueDao.updateOrder(item.itemId!!, positionOrder)
            } else {
                playbackQueueDao.moveRangeUp(item.order, positionOrder, 1)
                playbackQueueDao.updateOrder(item.itemId!!, positionOrder)
            }
        }
    }

    fun deleteItemId(itemId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            needAutoSave.postValue(true)
            playbackQueueDao.deleteByItemId(itemId)
        }
    }

    val currentPosition = MutableLiveData<Int>().apply {
        postValue(0)
    }

    private val _playing = MutableLiveData<Boolean>()

    val playing: LiveData<Boolean> = _playing

    private var _beginSongItemId = MutableLiveData<Long>().apply {
        postValue(NONE_SONG_ITEM_ID)
    }

    private var waitBeginRecord = false

    val playOnceSongName: LiveData<String?> = _beginSongItemId.switchMap {
        if (it == NONE_SONG_ITEM_ID) {
            liveData {
                emit(null)
            }
        } else {
            liveData {
                withContext(Dispatchers.IO) {
                    val song = playbackQueueDao.getSongSync(it)
                    if (song != null) {
                        emit(song.song.name)
                    }
                }
            }
        }
    }

    fun emit(event: PlaybackEvent, arg: Any? = null) {
        Log.d(TAG, "emit $event $arg")
        when (event) {
            PlaybackEvent.PLAY -> {
                _playing.postValue(true)
                if (_currentItemId.value == null || _currentItemId.value == NONE_SONG_ITEM_ID) {
                    nextSong()
                } else {
                    _event.postValue(PlaybackEvent.PLAY)
                }
            }
            PlaybackEvent.PAUSE -> {
                _playing.postValue(false)
                _event.postValue(PlaybackEvent.PAUSE)
            }
            PlaybackEvent.STOP -> {
                _playing.postValue(false)
                _currentItemId.postValue(NONE_SONG_ITEM_ID)
                _event.postValue(PlaybackEvent.STOP)
            }
            PlaybackEvent.PLAY_PAUSE -> {
                if (_playing.value == true) {
                    emit(PlaybackEvent.PAUSE)
                } else {
                    emit(PlaybackEvent.PLAY)
                }
            }
            PlaybackEvent.SKIP_PREV_SONG -> {
                previousSong()
            }
            PlaybackEvent.SKIP_NEXT_SONG -> {
                nextSong()
            }
            PlaybackEvent.SONG_PREPARED -> {
                if (_playing.value == true) {
                    _event.postValue(PlaybackEvent.PLAY)
                }
            }
            PlaybackEvent.SONG_FINISHED -> {
                nextSong()
            }
            PlaybackEvent.CHOOSE_SONG_AND_PLAY -> {
                if (arg != null && arg is Long) {
                    _playing.postValue(true)
                    _currentItemId.postValue(arg)
                }
            }
            PlaybackEvent.PREPARE_FOR_NEW_SONG -> {

            }
        }
    }

    fun emit(event: AppEvent) {
        Log.d(TAG, "emit $event")
        when (event) {
            AppEvent.PLAY_ONCE_BEGIN_RECORD -> {
                Log.d(TAG, "emit $event on ${_currentItemId.value}")
                if (_currentItemId.value == NONE_SONG_ITEM_ID) {
                    waitBeginRecord = true
                } else {
                    _beginSongItemId.postValue(_currentItemId.value)
                    waitBeginRecord = false
                }
            }
        }
    }

    private val _event = SingleLiveEvent<PlaybackEvent>()

    val event: LiveData<PlaybackEvent> = _event
}