package puelloc.musicplayer.ui.dialog

import android.annotation.SuppressLint
import android.app.Dialog
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothSocket
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.fragment.app.DialogFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import puelloc.musicplayer.R
import puelloc.musicplayer.databinding.DialogMessageBinding
import puelloc.musicplayer.service.AudioCaptureService
import puelloc.musicplayer.utils.BuiltinSetting
import puelloc.musicplayer.utils.VersionUtil.Companion.ANDROID_10
import java.io.IOException

@RequiresApi(ANDROID_10)
class BluetoothClientDialog(private val bluetoothDevice: BluetoothDevice) : DialogFragment() {
    private lateinit var bluetoothAdapter: BluetoothAdapter
    private lateinit var thread: ConnectThread
    private lateinit var binding: DialogMessageBinding
    private var audioCaptureService: AudioCaptureService? = null


    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val bluetoothManager =
            requireContext().getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothAdapter = bluetoothManager.adapter

        binding = DialogMessageBinding.inflate(requireActivity().layoutInflater)
        binding.message.setText(R.string.wait_for_audio_capture)
        thread = ConnectThread(bluetoothDevice)

        requireActivity().bindService(
            Intent(requireContext(), AudioCaptureService::class.java),
            object : ServiceConnection {
                override fun onServiceConnected(p0: ComponentName?, binder: IBinder?) {
                    val myBinder = binder as AudioCaptureService.MyBinder
                    audioCaptureService = myBinder.getService()
                    binding.message.setText(R.string.audio_capture_connected)
                    thread.start()
                }

                override fun onServiceDisconnected(p0: ComponentName?) {
                    thread.cancel()
                    binding.message.setText(R.string.audio_capture_disconnected)
                }
            },
            0
        )

        return activity?.let { activity ->
            val dialog = MaterialAlertDialogBuilder(activity)
                .setTitle(R.string.audio_transfer)
                .setView(binding.root)
                .setNeutralButton(R.string.cancel, null)
                .setPositiveButton(R.string.ok, null)
                .create()
            dialog
        } ?: throw IllegalStateException("Activity cannot be null")
    }

    override fun onDestroy() {
        super.onDestroy()
        thread.cancel()
    }

    @SuppressLint("MissingPermission")
    private inner class ConnectThread(device: BluetoothDevice) : Thread() {

        private val socket: BluetoothSocket? by lazy(LazyThreadSafetyMode.NONE) {
            when (BuiltinSetting.BLUETOOTH_USE) {
                BuiltinSetting.BluetoothProtocols.RFCOMM -> device.createRfcommSocketToServiceRecord(
                    BuiltinSetting.BLUETOOTH_RFCOMM_UUID
                )
                BuiltinSetting.BluetoothProtocols.RFCOMM_INSECURE -> device.createInsecureRfcommSocketToServiceRecord(
                    BuiltinSetting.BLUETOOTH_RFCOMM_UUID
                )
            }
        }

        override fun run() {
            socket?.let { socket ->
                try {
                    socket.connect()
                    binding.message.post {
                        binding.message.setText(R.string.connected_audio_receiver)
                    }
                    audioCaptureService?.socket = socket
                    audioCaptureService?.onSocketError = {
                        binding.message.post {
                            binding.message.setText(R.string.audio_receiver_disconnected)
                        }
                    }
                } catch (e: IOException) {
                    binding.message.post {
                        binding.message.setText(R.string.connected_audio_receiver_fail)
                    }
                    cancel()
                }
            }
        }

        fun cancel() {
            try {
                audioCaptureService?.socket = null
                socket?.close()
            } catch (e: IOException) {
                Log.e(TAG, "Could not close the client socket", e)
            }
        }
    }

    companion object {
        private const val TAG = "BluetoothClientDialog"
    }
}