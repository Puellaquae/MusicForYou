package puelloc.musicplayer.utils

import android.os.Build

class VersionUtil {
    companion object {
        val Q: Boolean get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q

        val O: Boolean get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O

        val S: Boolean get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    }
}