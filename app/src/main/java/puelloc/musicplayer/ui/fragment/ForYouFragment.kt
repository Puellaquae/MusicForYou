package puelloc.musicplayer.ui.fragment

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.Application
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.fragment.app.Fragment
import puelloc.musicplayer.BuildConfig
import puelloc.musicplayer.R
import puelloc.musicplayer.adapter.NoDiffItemAdapter
import puelloc.musicplayer.databinding.FragmentForYouBinding
import puelloc.musicplayer.service.AudioCaptureService
import puelloc.musicplayer.ui.dialog.BluetoothClientDialog
import puelloc.musicplayer.ui.dialog.BluetoothListenerDialog
import puelloc.musicplayer.ui.dialog.BluetoothListenerDialogOld
import puelloc.musicplayer.ui.dialog.NFCDialog
import puelloc.musicplayer.ui.viewholder.SimpleItemViewHolder
import puelloc.musicplayer.utils.PermissionUtil.Companion.hasPermission
import puelloc.musicplayer.utils.PermissionUtil.Companion.requirePermissionResult
import puelloc.musicplayer.utils.VersionUtil
import puelloc.musicplayer.utils.VersionUtil.Companion.ANDROID_10
import puelloc.musicplayer.viewmodel.PlaybackQueueViewModel
import java.util.*

class ForYouFragment : Fragment() {
    private var _binding: FragmentForYouBinding? = null
    private var binding: FragmentForYouBinding
        get() = _binding!!
        set(value) {
            _binding = value
        }

    private lateinit var playbackQueueViewModel: PlaybackQueueViewModel
    private var audioCapturing = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentForYouBinding.inflate(inflater, container, false)
        playbackQueueViewModel =
            PlaybackQueueViewModel.getInstance(requireContext().applicationContext as Application)
        return binding.root
    }

    @RequiresApi(ANDROID_10)
    private fun startAudioCapture(data: Intent?) {
        audioCapturing = true
        binding.buttonCapture.setText(R.string.stop_audio_capture)
        requireContext().startService(
            Intent(
                requireContext(),
                AudioCaptureService::class.java
            ).apply {
                action = AudioCaptureService.ACTION_START
                putExtra(AudioCaptureService.EXTRA_RESULT_DATA, data)
            })
    }

    private lateinit var requireAudioCapture: ActivityResultLauncher<Intent>
    private lateinit var requireAudioRecord: ActivityResultLauncher<String>
    private lateinit var requireBluetoothConnect: ActivityResultLauncher<String>
    private lateinit var requireBluetoothConnectListener: ActivityResultLauncher<String>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (VersionUtil.Q) {
            requireAudioCapture =
                registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
                    if (it.resultCode == Activity.RESULT_OK) {
                        startAudioCapture(it.data)
                    } else {
                        Toast.makeText(
                            requireContext(),
                            getString(R.string.permission_denied, "AUDIO RECORD"),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            requireAudioRecord = requirePermissionResult {
                if (it) {
                    requireAudioCapture()
                } else {
                    Toast.makeText(
                        requireContext(),
                        getString(R.string.permission_denied, "RECORD_MUSIC"),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
            requireBluetoothConnect = requirePermissionResult { got ->
                if (got) {
                    loadBluetoothBound()
                } else {
                    showToast(getString(R.string.permission_denied, "BLUETOOTH_CONNECT"))
                }
            }
            requireBluetoothConnectListener = requirePermissionResult {
                if (it) {
                    BluetoothListenerDialog().show(
                        parentFragmentManager,
                        "BluetoothListenerDialog"
                    )
                } else {
                    showToast(getString(R.string.permission_denied, "BLUETOOTH_CONNECT"))
                }
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun loadBluetoothBound() {
        val bluetoothManager =
            requireContext().getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        val bluetoothAdapter = bluetoothManager.adapter
        val adapter = NoDiffItemAdapter(
            bluetoothAdapter.bondedDevices.toList(),
            SimpleItemViewHolder<BluetoothDevice>(
                { it.name },
                { it.address },
                { R.drawable.ic_baseline_bluetooth_24 },
                R.drawable.ic_baseline_bluetooth_24,
            ) { bluetoothDevice ->
                if (VersionUtil.Q) {
                    BluetoothClientDialog(bluetoothDevice).show(
                        parentFragmentManager,
                        "BluetoothClientDialog"
                    )
                } else {
                    showToast(getString(R.string.only_support_android_above, 10))
                }
            }
        )
        binding.rawDataList.adapter = adapter
    }

    @SuppressLint("MissingPermission", "InlinedApi")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.textView.text =
            getString(R.string.build_time, Date(BuildConfig.BUILD_TIME.toLong()).toString())
        binding.button.setOnClickListener {
            NFCDialog().show(parentFragmentManager, "NFC Dialog")
        }
        binding.buttonCapture.setOnClickListener {
            if (VersionUtil.Q) {
                if (audioCapturing) {
                    stopAudioCapture()
                } else {
                    if (hasPermission(Manifest.permission.RECORD_AUDIO)) {
                        requireAudioCapture()
                    } else {
                        requireAudioRecord.launch(Manifest.permission.RECORD_AUDIO)
                    }
                }
            } else {
                showToast(getString(R.string.only_support_android_above, 10))
            }
        }
        binding.buttonBluetooth.setOnClickListener {
            if (VersionUtil.S) {
                requireBluetoothConnectListener.launch(Manifest.permission.BLUETOOTH_CONNECT)
            } else {
                BluetoothListenerDialog().show(
                    parentFragmentManager,
                    "BluetoothListenerDialog"
                )
            }
        }
        binding.buttonBluetoothOld.setOnClickListener {
            if (VersionUtil.S) {
                requireBluetoothConnectListener.launch(Manifest.permission.BLUETOOTH_CONNECT)
            } else {
                BluetoothListenerDialogOld().show(
                    parentFragmentManager,
                    "BluetoothListenerDialog"
                )
            }
        }
        binding.rawDataLabel.text = getString(R.string.bluetooth_bound)
        if (VersionUtil.S) {
            requireBluetoothConnect.launch(Manifest.permission.BLUETOOTH_CONNECT)
        } else {
            loadBluetoothBound()
        }
    }

    @RequiresApi(ANDROID_10)
    private fun stopAudioCapture() {
        requireContext().startService(
            Intent(
                requireContext(),
                AudioCaptureService::class.java
            ).apply { action = AudioCaptureService.ACTION_STOP })
        audioCapturing = false
        binding.buttonCapture.setText(R.string.start_audio_capture)
    }

    @RequiresApi(ANDROID_10)
    private fun requireAudioCapture() {
        val mediaProjectionManager =
            requireContext().applicationContext.getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        requireAudioCapture.launch(mediaProjectionManager.createScreenCaptureIntent())
    }

    private fun showToast(message: String) {
        Toast.makeText(
            requireContext(),
            message,
            Toast.LENGTH_SHORT
        ).show()
    }
}