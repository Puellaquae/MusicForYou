package puelloc.musicplayer.ui.dialog

import android.annotation.SuppressLint
import android.app.Dialog
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.content.DialogInterface
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.fragment.app.DialogFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import puelloc.musicplayer.R
import puelloc.musicplayer.databinding.DialogNfcBinding
import puelloc.musicplayer.ui.fragment.ForYouFragment
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import kotlin.random.Random

class BluetoothClientDialog(val bluetoothDevice: BluetoothDevice) : DialogFragment() {
    private lateinit var bluetoothAdapter: BluetoothAdapter
    private lateinit var thread: ConnectThread
    private lateinit var binding: DialogNfcBinding

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val bluetoothManager =
            requireContext().getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothAdapter = bluetoothManager.adapter

        binding = DialogNfcBinding.inflate(requireActivity().layoutInflater)
        thread = ConnectThread(bluetoothDevice)
        thread.start()
        return activity?.let { activity ->
            val dialog = MaterialAlertDialogBuilder(activity)
                .setTitle("Connecting")
                .setView(binding.root)
                .setNeutralButton(R.string.cancel) { _, _ ->
                    // Do Nothing
                }.setPositiveButton(R.string.ok) { _, _ ->
                    // Do Nothing
                }.create()
            dialog
        } ?: throw IllegalStateException("Activity cannot be null")
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        thread.cancel()
    }

    val bluetoothService =
        MyBluetoothService(Handler(Looper.getMainLooper()) {
            binding.message.text = "${it.what} ${it.arg1} ${it.obj}"
            true
        })

    @SuppressLint("MissingPermission")
    private inner class ConnectThread(device: BluetoothDevice) : Thread() {

        private val mmSocket: BluetoothSocket? by lazy(LazyThreadSafetyMode.NONE) {
            device.createRfcommSocketToServiceRecord(ForYouFragment.uuid)
        }

        override fun run() {
            // Cancel discovery because it otherwise slows down the connection.

            mmSocket?.let { socket ->
                // Connect to the remote device through the socket. This call blocks
                // until it succeeds or throws an exception.
                socket.connect()

                // The connection attempt succeeded. Perform work associated with
                // the connection in a separate thread.
                bluetoothService.start(socket)
            }
        }

        // Closes the client socket and causes the thread to finish.
        fun cancel() {
            try {
                mmSocket?.close()
                bluetoothService.close()
            } catch (e: IOException) {
                Log.e(TAG, "Could not close the client socket", e)
            }
        }
    }

    class MyBluetoothService(
        // handler that gets info from Bluetooth service
        private val handler: Handler
    ) {

        private inner class ConnectedThread(private val mmSocket: BluetoothSocket) : Thread() {

            private val mmOutStream: OutputStream = mmSocket.outputStream
            private val mmBuffer: ByteArray = ByteArray(1024) // mmBuffer store for the stream
            val random = Random(123)
            var running = true

            override fun run() {
                while (running) {
                    sleep(500)
                    write((0..(0..123).random(random)).map { ('a'..'z').random(random) }
                        .joinToString().toByteArray(charset = Charsets.UTF_8))
                }
            }

            // Call this from the main activity to send data to the remote device.
            fun write(bytes: ByteArray) {
                try {
                    mmOutStream.write(bytes)
                } catch (e: IOException) {
                    Log.e(TAG, "Error occurred when sending data", e)

                    // Send a failure message back to the activity.
                    val writeErrorMsg = handler.obtainMessage(MESSAGE_TOAST)
                    val bundle = Bundle().apply {
                        putString("toast", "Couldn't send data to the other device")
                    }
                    writeErrorMsg.data = bundle
                    handler.sendMessage(writeErrorMsg)
                    running = false
                    return
                }

                // Share the sent message with the UI activity.
                val writtenMsg = handler.obtainMessage(
                    MESSAGE_WRITE, bytes.size, -1, bytes
                )
                writtenMsg.sendToTarget()
            }

            // Call this method from the main activity to shut down the connection.
            fun cancel() {
                try {
                    running = false
                    mmSocket.close()
                } catch (e: IOException) {
                    Log.e(TAG, "Could not close the connect socket", e)
                }
            }
        }

        private var thread: ConnectedThread? = null

        fun start(socket: BluetoothSocket) {
            thread = ConnectedThread(socket)
            thread?.start()
        }

        fun close() {
            thread?.cancel()
        }
    }

    companion object {
        const val TAG = "BluetoothClientDialog"
        const val MESSAGE_READ: Int = 0
        const val MESSAGE_WRITE: Int = 1
        const val MESSAGE_TOAST: Int = 2
    }
}