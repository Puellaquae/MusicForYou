package puelloc.musicplayer.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.asLiveData
import puelloc.musicplayer.db.AppDatabase
import puelloc.musicplayer.entity.Playlist
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
                    subPlaylist.add(PlaylistWithSongs(Playlist(name = path, isFromFolder = true), subSongs))
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
        playlistDao.clearPlaylistsWhichBuiltFromFolder()
        playlistDao.insertPlaylistsWithSons(playlists)
    }
}