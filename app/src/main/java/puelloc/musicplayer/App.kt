package puelloc.musicplayer

import android.app.Application
import com.google.android.material.color.DynamicColors


class App : Application() {
    companion object {
        const val PLAYLIST_ID_MESSAGE = "puelloc.musicplayer@playlistId"
    }

    override fun onCreate() {
        super.onCreate()
        DynamicColors.applyToActivitiesIfAvailable(this)
    }
}