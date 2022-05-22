package puelloc.musicplayer.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import puelloc.musicplayer.db.AppDatabase
import puelloc.musicplayer.entity.Playlist
import puelloc.musicplayer.entity.PlaylistSongCrossRef
import puelloc.musicplayer.entity.PlaylistWithSongs
import puelloc.musicplayer.entity.Song

class PlaylistViewModel(application: Application) : AndroidViewModel(application) {
    private val appDatabase =
        AppDatabase.getDatabase(getApplication<Application?>().applicationContext)
    private val playlistDao = appDatabase.playlistDao()

    val playlists: LiveData<List<Playlist>> = playlistDao.getAllPlaylist().asLiveData()

    val playlistsWithSongs: LiveData<List<PlaylistWithSongs>> =
        playlistDao.getAllPlaylistsWithSongs().asLiveData()

    fun getPlaylistWithSong(playlistId: Long): LiveData<PlaylistWithSongs> =
        playlistDao.getPlaylistWithSongs(playlistId).asLiveData()

    fun getPlaylist(playlistId: Long): LiveData<Playlist> =
        playlistDao.getPlaylist(playlistId).asLiveData()

    /**
     * rebuild the playlist from folder and update the diff.
     * The deleted song **won't** delete from existed playlist in this function, it has been done in [SongViewModel.loadSongsSync].
     */
    fun buildPlaylistByDirSync() {
        data class Dir(
            val path: String,
            val songs: MutableList<Song>,
            val subDirs: MutableMap<String, Dir>
        ) {
            fun add(song: Song) {
                val paths = song.path.split('/').dropLast(1)
                Log.d("SongPath", "$paths")
                var curDir = this
                for (path in paths) {
                    curDir = curDir.subDirs.getOrPut(path) { Dir(path, ArrayList(), HashMap()) }
                }
                curDir.songs.add(song)
            }

            fun buildPlaylist(): List<PlaylistWithSongs> {
                val subPlaylist = ArrayList<PlaylistWithSongs>()
                val subSongs = ArrayList<Song>()
                subDirs.map { it.value.buildPlaylist() }.forEach {
                    subPlaylist.addAll(it)
                    subSongs.addAll(it.flatMap { p -> p.songs })
                }
                if (songs.isNotEmpty()) {
                    subSongs.addAll(songs)
                    subPlaylist.add(
                        PlaylistWithSongs(
                            Playlist(name = path, isFromFolder = true),
                            subSongs
                        )
                    )
                }
                return subPlaylist
            }
        }

        val dir = Dir("", ArrayList(), HashMap())
        val songs = appDatabase.songDao().getAllSongsSync()
        songs.forEach {
            dir.add(it)
        }
        val playlists = dir.buildPlaylist()
        playlistDao.insertOrUpdatePlaylistsWithSonsFromFolderStable(playlists)
    }

    fun newPlaylist(name: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val playlist = Playlist(name = name)
            playlistDao.insert(playlist)
        }
    }

    fun newPlaylistAndGetPlaylistId(name: String): LiveData<Long> =
        liveData {
            emit(withContext(viewModelScope.coroutineContext + Dispatchers.IO) {
                playlistDao.rowIdToPlaylistId(
                    playlistDao.insert(
                        Playlist(name = name)
                    )
                )
            })
        }

    fun removeSongsBySongIdFromPlaylistWithPlaylistId(playlistId: Long, songIds: List<Long>) {
        viewModelScope.launch(Dispatchers.IO) {
            songIds.forEach {
                playlistDao.removeSongFromPlaylist(playlistId, it)
            }
        }
    }

    fun getAllNotFromFolderPlaylistsWithSongs() =
        playlistDao.getAllNotFromFolderPlaylistsWithSongs().asLiveData()

    fun addSongsBySongIdToPlaylistWithPlaylistId(playlistId: Long, songIds: List<Long>) {
        viewModelScope.launch(Dispatchers.IO) {
            songIds.forEach {
                playlistDao.insertOrUpdateToRefPlaylistAndSong(
                    PlaylistSongCrossRef(playlistId, it)
                )
            }
        }
    }

    fun deletePlaylistsByPlaylistId(
        playlistIds: List<Long>,
        includeFolderPlaylist: Boolean = true
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            if (includeFolderPlaylist) {
                playlistIds
            } else {
                playlistDao.excludeFolderPlaylistIdSync(playlistIds)
            }.forEach {
                playlistDao.deleteToDeRefAllSongsOfPlaylist(it)
                playlistDao.deleteByPlaylistId(it)
            }
        }
    }
}