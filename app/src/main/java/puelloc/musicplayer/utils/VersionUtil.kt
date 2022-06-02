package puelloc.musicplayer.utils

import android.os.Build

class VersionUtil {
    companion object {
        const val ANDROID_10 = Build.VERSION_CODES.Q

        /**
         * Android 10
         */
        val Q: Boolean get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q

        const val ANDROID_11 = Build.VERSION_CODES.O

        /**
         * Android 11
         */
        val O: Boolean get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
        const val ANDROID_12 = Build.VERSION_CODES.S

        /**
         * Android 12
         */
        val S: Boolean get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    }
}