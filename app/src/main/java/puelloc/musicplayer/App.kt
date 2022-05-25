package puelloc.musicplayer

import android.app.Application
import android.content.Intent
import com.google.android.material.color.DynamicColors
import puelloc.musicplayer.service.MediaPlaybackService

class App : Application() {
    override fun onCreate() {
        super.onCreate()
        DynamicColors.applyToActivitiesIfAvailable(this)
        startService(Intent(this, MediaPlaybackService::class.java))
    }
}