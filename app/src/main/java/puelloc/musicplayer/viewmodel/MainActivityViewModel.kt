package puelloc.musicplayer.viewmodel

import android.app.Application
import androidx.lifecycle.*
import kotlinx.coroutines.flow.collect
import puelloc.musicplayer.R
import puelloc.musicplayer.db.AppDatabase

class MainActivityViewModel(application: Application) : AndroidViewModel(application) {
    private val appDatabase =
        AppDatabase.getDatabase(getApplication<Application?>().applicationContext)
    private val playlistDao = appDatabase.playlistDao()

    private val _showPlaylistId = MutableLiveData<Long>().apply {
        postValue(-1)
    }

    val showPlaylistId: LiveData<Long> = _showPlaylistId

    fun setToShowPlaylist(playlistId: Long) {
        _showPlaylistId.value = playlistId
    }

    private val _currentFragmentRes = MutableLiveData<Int>().apply {
        postValue(R.id.nav_song)
    }

    val currentFragmentRes: LiveData<Int> = _currentFragmentRes

    fun setCurrentFragmentRes(idx: Int) {
        _currentFragmentRes.value = idx
    }

    val currentMusicLibraryTitle = _currentFragmentRes.switchMap { res ->
        val title =
            getApplication<Application?>().applicationContext.getString(R.string.app_name)
        when (res) {
            R.id.nav_playlist -> _showPlaylistId.switchMap { playlistId ->
                when (playlistId) {
                    SHOW_MUSIC_LIBRARY -> liveData {
                        emit(title to "")
                    }
                    else -> playlistDao.getPlaylistWithSongs(playlistId).asLiveData()
                        .map { playlistWithSongs ->
                            playlistWithSongs.playlist.name to "${playlistWithSongs.songs.size} songs"
                        }
                }
            }
            else -> liveData { emit(title to "") }
        }
    }

    companion object {
        const val SHOW_MUSIC_LIBRARY = -1L
    }
}