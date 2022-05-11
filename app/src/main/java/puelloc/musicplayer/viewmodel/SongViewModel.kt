package puelloc.musicplayer.viewmodel

import android.app.Application
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.provider.MediaStore
import android.util.Log
import androidx.lifecycle.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import puelloc.musicplayer.entity.Song

class SongViewModel(application: Application) : AndroidViewModel(application) {
    private val songs = MutableLiveData<List<Song>>()

    fun getSongs(): LiveData<List<Song>> = songs

    fun getCoverSync(path: String): Bitmap? {
        val mediaMetadataRetriever = MediaMetadataRetriever()
        mediaMetadataRetriever.setDataSource(path)
        val coverData = mediaMetadataRetriever.embeddedPicture
        return if (coverData != null) {
            val bitmap = BitmapFactory.decodeByteArray(coverData, 0, coverData.size)
            bitmap
        } else {
            null
        }
    }

    fun getCover(path: String): LiveData<Bitmap> {
        return liveData(Dispatchers.IO) {
            getCoverSync(path)?.let { emit(it) }
        }
    }

    fun loadSongs() {
        viewModelScope.launch(Dispatchers.IO) {
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
                        cursor.getString(idIdx),
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

            songsArray.forEach {
                it.cover = getCoverSync(it.path)
            }

            songs.postValue(songsArray)
        }
    }
}