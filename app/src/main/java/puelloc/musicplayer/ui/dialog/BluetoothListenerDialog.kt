package puelloc.musicplayer.ui.dialog

import android.annotation.SuppressLint
import android.app.Dialog
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothServerSocket
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.media.*
import android.os.Bundle
import android.os.Process.THREAD_PRIORITY_AUDIO
import android.os.Process.setThreadPriority
import android.util.Log
import androidx.fragment.app.DialogFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import puelloc.musicplayer.R
import puelloc.musicplayer.databinding.DialogMessageBinding
import puelloc.musicplayer.utils.AACUtils
import puelloc.musicplayer.utils.BuiltinSetting.BluetoothProtocols.RFCOMM
import puelloc.musicplayer.utils.BuiltinSetting.BluetoothProtocols.RFCOMM_INSECURE
import puelloc.musicplayer.utils.BuiltinSetting.Companion.AUDIO_CAPTURE_SAMPLE_RATE
import puelloc.musicplayer.utils.BuiltinSetting.Companion.BLUETOOTH_RFCOMM_UUID
import puelloc.musicplayer.utils.BuiltinSetting.Companion.BLUETOOTH_USE
import puelloc.musicplayer.utils.BuiltinSetting.Companion.BUFFER_SIZE_IN_BYTES
import java.io.IOException
import java.io.InputStream
import java.nio.ByteBuffer
import java.util.concurrent.ConcurrentLinkedQueue


class BluetoothListenerDialog : DialogFragment() {
    private lateinit var bluetoothAdapter: BluetoothAdapter
    private lateinit var acceptThread: AcceptThread
    private var connectedThread: ConnectedThread? = null
    private lateinit var binding: DialogMessageBinding

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val bluetoothManager =
            requireContext().getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothAdapter = bluetoothManager.adapter

        binding = DialogMessageBinding.inflate(requireActivity().layoutInflater)
        binding.message.setText(R.string.wait_for_audio_transfer)
        acceptThread = AcceptThread()
        acceptThread.start()

        return activity?.let { activity ->
            val dialog = MaterialAlertDialogBuilder(activity)
                .setTitle(R.string.audio_receiver)
                .setView(binding.root)
                .setNeutralButton(R.string.cancel, null)
                .setPositiveButton(R.string.ok, null).create()
            dialog
        } ?: throw IllegalStateException("Activity cannot be null")
    }

    override fun onDestroy() {
        super.onDestroy()
        acceptThread.cancel()
        connectedThread?.cancel()
    }

    @SuppressLint("MissingPermission")
    private inner class AcceptThread : Thread() {
        val serverSocket: BluetoothServerSocket? by lazy(LazyThreadSafetyMode.NONE) {
            when (BLUETOOTH_USE) {
                RFCOMM -> bluetoothAdapter.listenUsingRfcommWithServiceRecord(
                    "Music For You",
                    BLUETOOTH_RFCOMM_UUID
                )
                RFCOMM_INSECURE -> bluetoothAdapter.listenUsingRfcommWithServiceRecord(
                    "Music For You",
                    BLUETOOTH_RFCOMM_UUID
                )
            }
        }

        override fun run() {
            var shouldLoop = true
            while (shouldLoop) {
                val socket: BluetoothSocket? = try {
                    serverSocket?.accept()
                } catch (e: IOException) {
                    Log.e(TAG, "socket accept fail", e)
                    binding.message.post {
                        binding.message.setText(R.string.connected_audio_transfer_fail)
                    }
                    serverSocket?.close()
                    shouldLoop = false
                    null
                }
                socket?.also {
                    binding.message.post {
                        binding.message.setText(R.string.connected_audio_transfer)
                    }
                    connectedThread = ConnectedThread(it)
                    connectedThread?.start()
                    serverSocket?.close()
                    shouldLoop = false
                }
            }
        }

        fun cancel() {
            try {
                serverSocket?.close()
            } catch (e: IOException) {
                Log.e(TAG, "socket close fail", e)
            }
        }
    }

    private inner class ConnectedThread(private val socket: BluetoothSocket) : Thread() {
        private val inStream: InputStream = socket.inputStream.apply {
            buffered(BUFFER_SIZE_IN_BYTES * 4)
        }
        private val buffer: ByteArray = ByteArray(BUFFER_SIZE_IN_BYTES)
        private lateinit var audioTrack: AudioTrack
        private lateinit var mediaCodec: MediaCodec
        private val availableAAC = ConcurrentLinkedQueue<ByteArray>()
        private val availableInputIndex = ConcurrentLinkedQueue<Int>()
        private val availablePCM = ConcurrentLinkedQueue<ByteArray>()

        private var needStop = false

        private val playingThread = object : Thread() {
            override fun run() {
                setThreadPriority(THREAD_PRIORITY_AUDIO)
                while (!needStop) {
                    val data = availablePCM.poll()
                    if (data != null) {
                        audioTrack.write(data, 0, data.size)
                    }
                }
            }
        }

        override fun run() {
            setThreadPriority(THREAD_PRIORITY_AUDIO)
            mediaCodec =
                MediaCodec.createDecoderByType(MediaFormat.MIMETYPE_AUDIO_AAC)
            mediaCodec.setCallback(object : MediaCodec.Callback() {
                override fun onInputBufferAvailable(codec: MediaCodec, inputIndex: Int) {
                    val data = availableAAC.poll()
                    if (data == null) {
                        availableInputIndex.add(inputIndex)
                    } else {
                        val inputBuffer = codec.getInputBuffer(inputIndex)
                        inputBuffer?.clear()
                        inputBuffer?.put(data)
                        codec.queueInputBuffer(inputIndex, 0, data.size, 0, 0)
                    }
                }

                override fun onOutputBufferAvailable(
                    codec: MediaCodec,
                    outputIndex: Int,
                    bufferInfo: MediaCodec.BufferInfo
                ) {
                    val outputBuffer = codec.getOutputBuffer(outputIndex)
                    if (outputBuffer != null) {
                        val data = ByteArray(bufferInfo.size)
                        outputBuffer.position(bufferInfo.offset)
                        outputBuffer.limit(bufferInfo.offset + bufferInfo.size)
                        outputBuffer.get(data)
                        outputBuffer.position(bufferInfo.size)
                        availablePCM.add(data)
                    }
                    codec.releaseOutputBuffer(outputIndex, false)
                }

                override fun onError(p0: MediaCodec, e: MediaCodec.CodecException) {
                    Log.d(
                        TAG,
                        "mediaCodec ${e.diagnosticInfo} recover${e.isRecoverable} trans${e.isTransient}",
                        e
                    )
                }

                override fun onOutputFormatChanged(p0: MediaCodec, p1: MediaFormat) {
                    Log.d(
                        TAG,
                        "format ${p1.getString(MediaFormat.KEY_MIME)} ${p1.getInteger(MediaFormat.KEY_SAMPLE_RATE)}"
                    )
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
                        setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, BUFFER_SIZE_IN_BYTES * 4)
                        // setInteger(MediaFormat.KEY_IS_ADTS, 1)
                        setByteBuffer(
                            "csd-0",
                            ByteBuffer.wrap(arrayOf(0x12.toByte(), 0x10.toByte()).toByteArray())
                        )
                    },
                null,
                null,
                0
            )
            mediaCodec.start()
            audioTrack = AudioTrack.Builder().apply {
                setAudioAttributes(AudioAttributes.Builder().apply {
                    setUsage(AudioAttributes.USAGE_MEDIA)
                    setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    setLegacyStreamType(AudioManager.STREAM_MUSIC)
                }.build())
                setAudioFormat(AudioFormat.Builder().apply {
                    setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    setSampleRate(AUDIO_CAPTURE_SAMPLE_RATE)
                    setChannelMask(AudioFormat.CHANNEL_OUT_STEREO)
                }.build())
                setTransferMode(AudioTrack.MODE_STREAM)
                setBufferSizeInBytes(BUFFER_SIZE_IN_BYTES * 4)
            }.build()
            audioTrack.play()
            playingThread.start()
            while (true) {
                try {
                    var needRead = 7
                    var offset = 0
                    var actualRead = inStream.read(buffer, offset, needRead)
                    while (actualRead < needRead) {
                        offset += actualRead
                        needRead -= actualRead
                        actualRead = inStream.read(buffer, offset, needRead)
                    }
                    val frameSize = AACUtils.frameSize(buffer)
                    needRead = frameSize - 7
                    offset = 7
                    actualRead = inStream.read(buffer, offset, needRead)
                    while (actualRead < needRead) {
                        offset += actualRead
                        needRead -= actualRead
                        actualRead = inStream.read(buffer, offset, needRead)
                    }
                    val data = buffer.copyOfRange(7, frameSize)
                    val index = availableInputIndex.poll()
                    if (index != null) {
                        val inputBuffer = mediaCodec.getInputBuffer(index)
                        inputBuffer?.clear()
                        inputBuffer?.put(data)
                        mediaCodec.queueInputBuffer(index, 0, data.size, 0, 0)
                    } else {
                        availableAAC.add(data)
                    }
                } catch (e: IOException) {
                    binding.message.post {
                        binding.message.setText(R.string.audio_transfer_disconnected)
                    }
                    cancel()
                    Log.d(TAG, "Input stream was disconnected", e)
                    break
                }
            }
        }

        fun cancel() {
            try {
                needStop = true
                mediaCodec.flush()
                socket.close()
                audioTrack.stop()
                audioTrack.release()
                mediaCodec.stop()
                mediaCodec.release()
            } catch (e: Exception) {
                Log.e(TAG, "Could not close", e)
            }
        }
    }

    companion object {
        const val TAG = "BluetoothListenerDialog"
    }
}