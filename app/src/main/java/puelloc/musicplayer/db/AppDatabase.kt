package puelloc.musicplayer.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import puelloc.musicplayer.dao.PlaylistDao
import puelloc.musicplayer.dao.SongDao
import puelloc.musicplayer.entity.Playlist
import puelloc.musicplayer.entity.PlaylistSongCrossRef
import puelloc.musicplayer.entity.Song

@Database(entities = [Song::class, Playlist::class, PlaylistSongCrossRef::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun playlistDao(): PlaylistDao
    abstract fun songDao(): SongDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context? = null): AppDatabase {
            // if the INSTANCE is not null, then return it,
            // if it is, then create the database
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context!!.applicationContext,
                    AppDatabase::class.java,
                    "music_player_database"
                )
                    // Wipes and rebuilds instead of migrating if no Migration object.
                    // Migration is not part of this codelab.
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                // return instance
                instance
            }
        }
    }
}