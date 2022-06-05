package puelloc.musicplayer.service

import android.annotation.SuppressLint
import android.app.Activity
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.content.Intent
import android.media.*
import android.media.AudioRecord.READ_NON_BLOCKING
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Binder
import android.os.IBinder
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.collection.CircularArray
import androidx.core.app.NotificationCompat
import puelloc.musicplayer.R
import puelloc.musicplayer.utils.AACUtils
import puelloc.musicplayer.utils.BuiltinSetting.Companion.BUFFER_SIZE_IN_BYTES
import puelloc.musicplayer.utils.BuiltinSetting.Companion.AUDIO_CAPTURE_SAMPLE_RATE
import puelloc.musicplayer.utils.VersionUtil.Companion.ANDROID_10
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import kotlin.concurrent.thread

@RequiresApi(ANDROID_10)
class AudioCaptureService : Service() {

    private lateinit var mediaProjectionManager: MediaProjectionManager
    private var mediaProjection: MediaProjection? = null

    private lateinit var audioCaptureThread: Thread
    private var audioRecord: AudioRecord? = null
    private lateinit var mediaCodec: MediaCodec

    private val circularArray = CircularArray<ByteArray>()

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
            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
            .setSampleRate(AUDIO_CAPTURE_SAMPLE_RATE)
            .setChannelMask(AudioFormat.CHANNEL_IN_STEREO)
            .build()

        mediaCodec = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_AUDIO_AAC)
        mediaCodec.setCallback(object : MediaCodec.Callback() {
            override fun onInputBufferAvailable(codec: MediaCodec, inputIndex: Int) {
                if (!circularArray.isEmpty) {
                    val inputBuffer = codec.getInputBuffer(inputIndex)
                    val buffer = circularArray.popFirst()
                    inputBuffer?.clear()
                    inputBuffer?.put(buffer)
                    codec.queueInputBuffer(inputIndex, 0, buffer.size, 0, 0)
                    // Log.d(TAG, "mediacodec input ${buffer.size}")
                } else {
                    codec.queueInputBuffer(inputIndex, 0, 0, 0, 0)
                }
            }

            override fun onOutputBufferAvailable(
                codec: MediaCodec,
                outputIndex: Int,
                bufferInfo: MediaCodec.BufferInfo
            ) {
                val outputBuffer = codec.getOutputBuffer(outputIndex)
                if (outputBuffer != null) {
                    try {
                        val outData = if (false) {
                            val data = ByteArray(bufferInfo.size + 7)
                            outputBuffer.position(bufferInfo.offset)
                            outputBuffer.limit(bufferInfo.offset + bufferInfo.size)
                            outputBuffer.get(data, 7, bufferInfo.size)
                            outputBuffer.position(bufferInfo.offset)
                            AACUtils.addADTS(data)
                            data
                        } else {
                            val data = ByteArray(bufferInfo.size)
                            outputBuffer.position(bufferInfo.offset)
                            outputBuffer.limit(bufferInfo.offset + bufferInfo.size)
                            outputBuffer.get(data)
                            outputBuffer.position(bufferInfo.offset)
                            data
                        }
                        socket?.outputStream?.write(outData)
                        // Log.d(TAG, "send ${data.size}")
                    } catch (e: IOException) {
                        socket = null
                        onSocketError?.let { it(e) }
                        onSocketError = null
                        Log.w(TAG, "send to bluetooth socket fail", e)
                    }
                } else {
                    Log.d(TAG, "null output buffer")
                }
                outputBuffer?.clear()
                codec.releaseOutputBuffer(outputIndex, false)
            }

            override fun onError(p0: MediaCodec, e: MediaCodec.CodecException) {
                Log.e(TAG, "mediaCode", e)
                // TODO("Not yet implemented")
            }

            override fun onOutputFormatChanged(p0: MediaCodec, format: MediaFormat) {
                Log.d(TAG, "format: ${format.getByteBuffer("csd-0")}")
                // TODO("Not yet implemented")
            }

        })
        mediaCodec.configure(
            MediaFormat.createAudioFormat(
                MediaFormat.MIMETYPE_AUDIO_AAC,
                AUDIO_CAPTURE_SAMPLE_RATE,
                2
            )
                .apply {
                    setInteger(MediaFormat.KEY_BIT_RATE, 320000)
                    setInteger(
                        MediaFormat.KEY_AAC_PROFILE,
                        MediaCodecInfo.CodecProfileLevel.AACObjectLC
                    )
                    setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, BUFFER_SIZE_IN_BYTES * 2)
                },
            null,
            null,
            MediaCodec.CONFIGURE_FLAG_ENCODE
        )
        mediaCodec.start()
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
                audioRecord?.read(
                    capturedAudioSamples,
                    0,
                    BUFFER_SIZE_IN_BYTES
                )
                    ?: 0
            circularArray.addLast(capturedAudioSamples.copyOf(numBytes))
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

        mediaCodec.stop()
        mediaCodec.release()

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