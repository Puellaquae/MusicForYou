package puelloc.musicplayer.service

import android.annotation.SuppressLint
import android.app.Activity
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioPlaybackCaptureConfiguration
import android.media.AudioRecord
import android.media.AudioRecord.READ_NON_BLOCKING
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Binder
import android.os.IBinder
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import puelloc.musicplayer.R
import puelloc.musicplayer.utils.BuiltinSetting.Companion.BUFFER_SIZE_IN_BYTES
import puelloc.musicplayer.utils.BuiltinSetting.Companion.USED_AUDIO_ENCODE
import puelloc.musicplayer.utils.BuiltinSetting.Companion.USED_SAMPLE_RATE
import puelloc.musicplayer.utils.VersionUtil.Companion.ANDROID_10
import java.io.IOException
import kotlin.concurrent.thread

@RequiresApi(ANDROID_10)
class AudioCaptureService : Service() {

    private lateinit var mediaProjectionManager: MediaProjectionManager
    private var mediaProjection: MediaProjection? = null

    private lateinit var audioCaptureThread: Thread
    private var audioRecord: AudioRecord? = null

    var socket: BluetoothSocket? = null
    var onSocketError: ((e: IOException) -> Unit)? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForeground(
            SERVICE_ID,
            NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID).build()
        )

        // use applicationContext to avoid memory leak on Android 10.
        // see: https://partnerissuetracker.corp.google.com/issues/139732252
        mediaProjectionManager =
            applicationContext.getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
    }

    private fun createNotificationChannel() {
        val serviceChannel = NotificationChannel(
            NOTIFICATION_CHANNEL_ID,
            getString(R.string.audio_capture_service),
            NotificationManager.IMPORTANCE_DEFAULT
        )

        val manager = getSystemService(NotificationManager::class.java) as NotificationManager
        manager.createNotificationChannel(serviceChannel)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return if (intent != null) {
            when (intent.action) {
                ACTION_START -> {
                    mediaProjection =
                        mediaProjectionManager.getMediaProjection(
                            Activity.RESULT_OK,
                            intent.getParcelableExtra(EXTRA_RESULT_DATA)!!
                        ) as MediaProjection
                    startAudioCapture()
                    START_STICKY
                }
                ACTION_STOP -> {
                    stopAudioCapture()
                    START_NOT_STICKY
                }
                else -> throw IllegalArgumentException("Unexpected action received: ${intent.action}")
            }
        } else {
            START_NOT_STICKY
        }
    }

    @SuppressLint("MissingPermission")
    private fun startAudioCapture() {
        val config = AudioPlaybackCaptureConfiguration.Builder(mediaProjection!!)
            .addMatchingUsage(AudioAttributes.USAGE_MEDIA)
            .build()

        /**
         * Using hardcoded values for the audio format, Mono PCM samples with a sample rate of 8000Hz
         * These can be changed according to your application's needs
         */
        val audioFormat = AudioFormat.Builder()
            .setEncoding(USED_AUDIO_ENCODE)
            .setSampleRate(USED_SAMPLE_RATE)
            .setChannelMask(AudioFormat.CHANNEL_IN_STEREO)
            .build()

        audioRecord = AudioRecord.Builder()
            .setAudioFormat(audioFormat)
            // For optimal performance, the buffer size
            // can be optionally specified to store audio samples.
            // If the value is not specified,
            // uses a single frame and lets the
            // native code figure out the minimum buffer size.
            .setBufferSizeInBytes(BUFFER_SIZE_IN_BYTES)
            .setAudioPlaybackCaptureConfig(config)
            .build()

        audioRecord!!.startRecording()
        audioCaptureThread = thread(start = true) {
            captureAudio()
        }
    }

    private fun captureAudio() {
        val capturedAudioSamples = ByteArray(BUFFER_SIZE_IN_BYTES)

        while (!audioCaptureThread.isInterrupted) {
            val numBytes =
                audioRecord?.read(capturedAudioSamples, 0, BUFFER_SIZE_IN_BYTES, READ_NON_BLOCKING)
                    ?: 0
            try {
                socket?.outputStream?.write(capturedAudioSamples, 0, numBytes)
            } catch (e: IOException) {
                socket = null
                onSocketError?.let { it(e) }
                onSocketError = null
                Log.w(TAG, "send to bluetooth socket fail", e)
            }
        }
    }

    private fun stopAudioCapture() {
        requireNotNull(mediaProjection) { "Tried to stop audio capture, but there was no ongoing capture in place!" }

        audioCaptureThread.interrupt()
        audioCaptureThread.join()

        audioRecord!!.stop()
        audioRecord!!.release()
        audioRecord = null

        mediaProjection!!.stop()

        socket = null
        onSocketError = null

        stopSelf()
    }

    inner class MyBinder : Binder() {
        fun getService(): AudioCaptureService = this@AudioCaptureService
    }

    private val binder = MyBinder()

    override fun onBind(p0: Intent?): IBinder {
        return binder
    }

    companion object {
        private const val TAG = "AudioCaptureService"
        private const val SERVICE_ID = 123
        private const val NOTIFICATION_CHANNEL_ID = "AudioCapture channel"

        const val ACTION_START = "AudioCaptureService:Start"
        const val ACTION_STOP = "AudioCaptureService:Stop"
        const val EXTRA_RESULT_DATA = "AudioCaptureService:Extra:ResultData"
    }
}