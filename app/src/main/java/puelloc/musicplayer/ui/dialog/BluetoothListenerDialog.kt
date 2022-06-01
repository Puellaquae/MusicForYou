package puelloc.musicplayer.ui.dialog

import android.annotation.SuppressLint
import android.app.Dialog
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothServerSocket
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

class BluetoothListenerDialog : DialogFragment() {
    private lateinit var bluetoothAdapter: BluetoothAdapter
    private lateinit var acceptThread: AcceptThread
    private lateinit var binding: DialogNfcBinding

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val bluetoothManager =
            requireContext().getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothAdapter = bluetoothManager.adapter

        binding = DialogNfcBinding.inflate(requireActivity().layoutInflater)
        acceptThread = AcceptThread()
        acceptThread.start()

        return activity?.let { activity ->
            val dialog = MaterialAlertDialogBuilder(activity)
                .setTitle("Listening")
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
        acceptThread.cancel()
    }

    val bluetoothService = MyBluetoothService(Handler(Looper.getMainLooper()) {
        binding.message.text = "${it.what} ${it.arg1} ${it.obj}"
        true
    })

    @SuppressLint("MissingPermission")
    private inner class AcceptThread : Thread() {
        val serverSocket: BluetoothServerSocket? by lazy(LazyThreadSafetyMode.NONE) {
            bluetoothAdapter.listenUsingRfcommWithServiceRecord(
                "Music For You",
                ForYouFragment.uuid
            )
        }

        override fun run() {
            var shouldLoop = true
            while (shouldLoop) {
                val socket: BluetoothSocket? = try {
                    Log.d(TAG, "try accept")
                    serverSocket?.accept()
                } catch (e: IOException) {
                    Log.e(TAG, "socket accept fail", e)
                    shouldLoop = false
                    null
                }
                socket?.also {
                    bluetoothService.start(it)
                    //serverSocket?.close()
                    shouldLoop = false
                }
            }
        }

        fun cancel() {
            try {
                serverSocket?.close()
                bluetoothService.close()
            } catch (e: IOException) {
                Log.e(TAG, "socket close fail", e)
            }
        }
    }

    class MyBluetoothService(
        // handler that gets info from Bluetooth service
        private val handler: Handler
    ) {

        private inner class ConnectedThread(private val mmSocket: BluetoothSocket) : Thread() {

            private val mmInStream: InputStream = mmSocket.inputStream
            private val mmBuffer: ByteArray = ByteArray(1024) // mmBuffer store for the stream

            override fun run() {
                var numBytes: Int // bytes returned from read()

                // Keep listening to the InputStream until an exception occurs.
                while (true) {
                    // Read from the InputStream.
                    numBytes = try {
                        mmInStream.read(mmBuffer)
                    } catch (e: IOException) {
                        Log.d(TAG, "Input stream was disconnected", e)
                        break
                    }

                    // Send the obtained bytes to the UI activity.
                    val readMsg = handler.obtainMessage(
                        MESSAGE_READ, numBytes, -1,
                        mmBuffer)
                    readMsg.sendToTarget()
                }
            }

            // Call this method from the main activity to shut down the connection.
            fun cancel() {
                try {
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
        const val TAG = "BluetoothListenerDialog"
        const val MESSAGE_READ: Int = 0
        const val MESSAGE_WRITE: Int = 1
        const val MESSAGE_TOAST: Int = 2
    }
}