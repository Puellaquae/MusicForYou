package puelloc.musicplayer.ui.dialog

import android.annotation.SuppressLint
import android.app.Dialog
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothServerSocket
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.DialogFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import puelloc.musicplayer.R
import puelloc.musicplayer.databinding.DialogMessageBinding
import puelloc.musicplayer.utils.BuiltinSetting.BluetoothProtocols.RFCOMM
import puelloc.musicplayer.utils.BuiltinSetting.BluetoothProtocols.RFCOMM_INSECURE
import puelloc.musicplayer.utils.BuiltinSetting.Companion.BLUETOOTH_RFCOMM_UUID
import puelloc.musicplayer.utils.BuiltinSetting.Companion.BLUETOOTH_USE
import puelloc.musicplayer.utils.BuiltinSetting.Companion.BUFFER_SIZE_IN_BYTES
import puelloc.musicplayer.utils.BuiltinSetting.Companion.USED_AUDIO_ENCODE
import puelloc.musicplayer.utils.BuiltinSetting.Companion.USED_SAMPLE_RATE
import java.io.IOException
import java.io.InputStream

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
        private val inStream: InputStream = socket.inputStream
        private val buffer: ByteArray =
            ByteArray(BUFFER_SIZE_IN_BYTES)
        private lateinit var audioTrack: AudioTrack

        override fun run() {
            audioTrack = AudioTrack.Builder().apply {
                setAudioAttributes(AudioAttributes.Builder().apply {
                    setUsage(AudioAttributes.USAGE_MEDIA)
                    setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    setLegacyStreamType(AudioManager.STREAM_MUSIC)
                }.build())
                setAudioFormat(AudioFormat.Builder().apply {
                    setEncoding(USED_AUDIO_ENCODE)
                    setSampleRate(USED_SAMPLE_RATE)
                    setChannelMask(AudioFormat.CHANNEL_OUT_STEREO)
                }.build())
                setTransferMode(AudioTrack.MODE_STREAM)
                setBufferSizeInBytes(BUFFER_SIZE_IN_BYTES)
            }.build()
            audioTrack.play()
            while (true) {
                try {
                    val numBytes = inStream.read(buffer)
                    audioTrack.write(buffer, 0, numBytes)
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
                socket.close()
                audioTrack.release()
            } catch (e: IOException) {
                Log.e(TAG, "Could not close the connect socket", e)
            }
        }
    }

    companion object {
        const val TAG = "BluetoothListenerDialog"
    }
}