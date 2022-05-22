package puelloc.musicplayer.viewmodel

import android.app.Application
import android.provider.MediaStore
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.asLiveData
import puelloc.musicplayer.db.AppDatabase
import puelloc.musicplayer.entity.Song

class SongViewModel(application: Application) : AndroidViewModel(application) {
    private val appDatabase =
        AppDatabase.getDatabase(getApplication<Application?>().applicationContext)
    private val songDao = appDatabase.songDao()
    private val playlistDao = appDatabase.playlistDao()

    fun getSongs(): LiveData<List<Song>> = songDao.getAllSongs().asLiveData()

    fun getSong(songId: Long): LiveData<Song> = songDao.getSong(songId).asLiveData()

    /**
     * Load song and update the diff, also it will update the song in playlist
     */
    fun loadSongsSync() {
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.DATA
        )
        val cursor = getApplication<Application>().applicationContext.contentResolver.query(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            projection,
            "${MediaStore.Audio.Media.IS_MUSIC} != 0 AND ${MediaStore.Audio.Media.DURATION} is not null",
            null,
            null
        )

        val songsArray = ArrayList<Song>()
        cursor?.apply {
            val idIdx = cursor.getColumnIndex(MediaStore.Audio.Media._ID)
            val titleIdx = cursor.getColumnIndex(MediaStore.Audio.Media.TITLE)
            val albumIdx = cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM)
            val artistIdx = cursor.getColumnIndex(MediaStore.Audio.Media.ARTIST)
            val durationIdx = cursor.getColumnIndex(MediaStore.Audio.Media.DURATION)
            val dataIdx = cursor.getColumnIndex(MediaStore.Audio.Media.DATA)
            while (cursor.moveToNext()) {
                val song = Song(
                    cursor.getLong(idIdx),
                    cursor.getString(titleIdx),
                    cursor.getString(albumIdx),
                    cursor.getString(artistIdx),
                    cursor.getString(dataIdx),
                    cursor.getLong(durationIdx)
                )
                songsArray.add(song)
            }
            cursor.close()
        }

        val songsNeedDelete = songDao.selectNotExistedSongIdSync(songsArray.map { it.songId })
        playlistDao.deleteSongs(songsNeedDelete)
        songDao.deleteSongs(songsNeedDelete)
        songDao.insertOrUpdate(songsArray)
    }
}