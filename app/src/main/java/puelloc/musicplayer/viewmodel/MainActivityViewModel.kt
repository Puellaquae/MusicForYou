package puelloc.musicplayer.viewmodel

import android.app.Application
import androidx.annotation.IdRes
import androidx.annotation.StringRes
import androidx.lifecycle.*
import puelloc.musicplayer.R
import puelloc.musicplayer.db.AppDatabase

class MainActivityViewModel(application: Application) : AndroidViewModel(application) {
    private val appDatabase =
        AppDatabase.getDatabase(getApplication<Application?>().applicationContext)
    private val playlistDao = appDatabase.playlistDao()
    private val playbackQueueViewModel =
        PlaybackQueueViewModel.getInstance(getApplication<Application?>().applicationContext as Application)

    private fun getString(@StringRes res: Int): String =
        getApplication<Application?>().applicationContext.getString(res)

    private fun getString(@StringRes res: Int, vararg args: Any): String =
        getApplication<Application?>().applicationContext.getString(res, *args)

    private val _showPlaylistId = MutableLiveData<Long>().apply {
        postValue(MUSIC_LIBRARY_SHOW_PLAYLISTS)
    }

    private val _currentFragmentRes = MutableLiveData<Int>().apply {
        postValue(R.id.nav_song)
    }

    private val _playlistSongsSelectionSize = MutableLiveData<Int>().apply {
        postValue(0)
    }

    private val _playlistsSelectionSize = MutableLiveData<Int>().apply {
        postValue(0)
    }

    val showPlaylistId: LiveData<Long> = _showPlaylistId

    fun setToShowPlaylist(playlistId: Long) {
        _showPlaylistId.value = playlistId
    }

    val currentFragmentRes: LiveData<Int> = _currentFragmentRes

    fun setCurrentFragmentRes(@IdRes menuId: Int) {
        _currentFragmentRes.value = menuId
    }

    val currentTitle = currentFragmentRes.switchMap { res ->
        val title =
            getApplication<Application?>().applicationContext.getString(R.string.app_name)
        when (res) {
            R.id.nav_music_library -> getTitleForMusicLibrary(title)
            else -> liveData { emit(title to "") }
        }
    }

    private fun getTitleForMusicLibrary(title: String): LiveData<Pair<String, String>> =
        showPlaylistId.switchMap { playlistId ->
            when (playlistId) {
                MUSIC_LIBRARY_SHOW_PLAYLISTS -> liveData {
                    emit(title to "")
                }
                else -> getTitleForShowPlaylist(playlistId)
            }
        }

    private fun getTitleForShowPlaylist(playlistId: Long): LiveData<Pair<String, String>> =
        playlistDao.getPlaylistWithSongs(playlistId)
            .switchMap {
                playlistSongsSelectionSize.map { size ->
                    if (size == 0) {
                        it.playlist.name to getString(R.string.songs_count, it.songs.size)
                    } else {
                        it.playlist.name to getString(
                            R.string.selected_songs_count_of_total,
                            size,
                            it.songs.size
                        )
                    }
                }
            }

    val currentTopBarButtonAndMenu: LiveData<Pair<Int?, Int?>> =
        currentFragmentRes.switchMap { res ->
            when (res) {
                R.id.nav_music_library -> getTopBarButtonAndMenuForMusicLibrary()
                R.id.nav_song -> liveData {
                    emit(null to R.menu.menu_playback_queue)
                }
                else -> liveData {
                    emit(null to null)
                }
            }
        }

    private fun getTopBarButtonAndMenuForMusicLibrary(): LiveData<Pair<Int?, Int?>> =
        showPlaylistId.switchMap { playlistId ->
            when (playlistId) {
                MUSIC_LIBRARY_SHOW_PLAYLISTS -> playlistsSelectionSize.map { size ->
                    if (size == 0) {
                        null to null
                    } else {
                        R.drawable.ic_baseline_close_24 to R.menu.music_playlists_selection
                    }
                }
                else -> playlistDao.getPlaylist(playlistId)
                    .switchMap { playlist ->
                        playlistSongsSelectionSize.map { size ->
                            if (size == 0) {
                                R.drawable.ic_baseline_arrow_back_24 to if (playlist.isFromFolder) {
                                    R.menu.playlist_folder
                                } else {
                                    R.menu.playlist
                                }
                            } else {
                                R.drawable.ic_baseline_close_24 to if (playlist.isFromFolder) {
                                    R.menu.playlist_folder_songs_selection
                                } else {
                                    R.menu.playlist_songs_selection
                                }
                            }
                        }
                    }
            }
        }

    val currentFABIcon: LiveData<Int?> = currentFragmentRes.switchMap { res ->
        when (res) {
            R.id.nav_music_library -> getFABIconForMusicLibrary()
            R.id.nav_song -> playbackQueueViewModel.currentSong.map {
                if (it == null) {
                    null
                } else {
                    R.drawable.ic_baseline_music_note_24
                }
            }
            else -> liveData { emit(null) }
        }
    }

    private fun getFABIconForMusicLibrary(): LiveData<Int?> =
        showPlaylistId.map {
            if (it == MUSIC_LIBRARY_SHOW_PLAYLISTS) {
                R.drawable.ic_baseline_add_24
            } else {
                R.drawable.ic_baseline_play_arrow_24
            }
        }

    val playlistSongsSelectionSize: LiveData<Int> = _playlistSongsSelectionSize

    fun setPlaylistSongsSelectionSize(size: Int) {
        _playlistSongsSelectionSize.value = size
    }

    val playlistsSelectionSize: LiveData<Int> = _playlistsSelectionSize

    fun setPlaylistsSelectionSize(size: Int) {
        _playlistsSelectionSize.value = size
    }

    companion object {
        const val MUSIC_LIBRARY_SHOW_PLAYLISTS = -1L
    }
}