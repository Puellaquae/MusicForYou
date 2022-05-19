package puelloc.musicplayer.viewmodel

import android.app.Application
import androidx.lifecycle.*
import puelloc.musicplayer.R
import puelloc.musicplayer.db.AppDatabase

class MainActivityViewModel(application: Application) : AndroidViewModel(application) {
    private val appDatabase =
        AppDatabase.getDatabase(getApplication<Application?>().applicationContext)
    private val playlistDao = appDatabase.playlistDao()

    private val _showPlaylistId = MutableLiveData<Long>().apply {
        postValue(-1)
    }

    private val _currentFragmentRes = MutableLiveData<Int>().apply {
        postValue(R.id.nav_song)
    }

    private val _selectionSize = MutableLiveData<Int>().apply {
        postValue(0)
    }

    val showPlaylistId: LiveData<Long> = _showPlaylistId

    fun setToShowPlaylist(playlistId: Long) {
        _showPlaylistId.value = playlistId
    }

    val currentFragmentRes: LiveData<Int> = _currentFragmentRes

    fun setCurrentFragmentRes(idx: Int) {
        _currentFragmentRes.value = idx
    }

    val currentTitle = _currentFragmentRes.switchMap { res ->
        val title =
            getApplication<Application?>().applicationContext.getString(R.string.app_name)
        when (res) {
            R.id.nav_playlist -> showPlaylistId.switchMap { playlistId ->
                when (playlistId) {
                    SHOW_MUSIC_LIBRARY -> liveData {
                        emit(title to "")
                    }
                    else -> getTitleForShowPlaylist(playlistId)
                }
            }
            else -> liveData { emit(title to "") }
        }
    }

    val currentTopBarButtonAndMenu: LiveData<Pair<Int?, Int?>> =
        _currentFragmentRes.switchMap { res ->
            when (res) {
                R.id.nav_playlist -> showPlaylistId.switchMap { playlistId ->
                    when (playlistId) {
                        SHOW_MUSIC_LIBRARY -> liveData {
                            emit(null to null)
                        }
                        else -> selectionSize.map { size ->
                            if (size == 0) {
                                R.drawable.ic_baseline_arrow_back_24 to null
                            } else {
                                R.drawable.ic_baseline_close_24 to R.menu.selection
                            }
                        }
                    }
                }
                else -> liveData {
                    emit(null to null)
                }
            }
        }

    private fun getTitleForShowPlaylist(playlistId: Long): LiveData<Pair<String, String>> {
        return playlistDao.getPlaylistWithSongs(playlistId).asLiveData()
            .switchMap {
                selectionSize.map { size ->
                    if (size == 0) {
                        it.playlist.name to "${it.songs.size} songs"
                    } else {
                        it.playlist.name to "${selectionSize.value} of ${it.songs.size} songs"
                    }
                }
            }
    }

    val selectionSize: LiveData<Int> = _selectionSize

    fun setSelectionSize(size: Int) {
        _selectionSize.value = size
    }

    companion object {
        const val SHOW_MUSIC_LIBRARY = -1L
    }
}