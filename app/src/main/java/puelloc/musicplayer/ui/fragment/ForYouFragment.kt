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
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import puelloc.musicplayer.BuildConfig
import puelloc.musicplayer.R
import puelloc.musicplayer.adapter.NoDiffItemAdapter
import puelloc.musicplayer.databinding.FragmentForYouBinding
import puelloc.musicplayer.service.AudioCaptureService
import puelloc.musicplayer.ui.dialog.BluetoothClientDialog
import puelloc.musicplayer.ui.dialog.BluetoothListenerDialog
import puelloc.musicplayer.ui.dialog.NFCDialog
import puelloc.musicplayer.utils.PermissionUtil.Companion.needPermission
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
    inner class MyLifecycleObserver : DefaultLifecycleObserver {
        lateinit var getResult: ActivityResultLauncher<Intent>
        override fun onCreate(owner: LifecycleOwner) {
            getResult = requireActivity().activityResultRegistry.register(
                "key",
                owner,
                ActivityResultContracts.StartActivityForResult()
            ) {
                if (it.resultCode == Activity.RESULT_OK) {
                    audioCapturing = true
                    binding.buttonCapture.setText(R.string.stop_audio_capture)
                    requireContext().startService(
                        Intent(
                            requireContext(),
                            AudioCaptureService::class.java
                        ).apply {
                            action = AudioCaptureService.ACTION_START
                            putExtra(AudioCaptureService.EXTRA_RESULT_DATA, it.data)
                        })
                } else {
                    Toast.makeText(
                        requireContext(),
                        getString(R.string.permission_denied, "AUDIO RECORD"),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    private lateinit var observer: MyLifecycleObserver

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (VersionUtil.Q) {
            observer = MyLifecycleObserver()
            lifecycle.addObserver(observer)
        }
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
                    requireContext().startService(
                        Intent(
                            requireContext(),
                            AudioCaptureService::class.java
                        ).apply { action = AudioCaptureService.ACTION_STOP })
                    audioCapturing = false
                    binding.buttonCapture.setText(R.string.start_audio_capture)
                } else {
                    needPermission(Manifest.permission.RECORD_AUDIO) {
                        if (it) {
                            val mediaProjectionManager =
                                requireContext().applicationContext.getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
                            observer.getResult.launch(mediaProjectionManager.createScreenCaptureIntent())
                        } else {
                            Toast.makeText(
                                requireContext(),
                                getString(R.string.permission_denied, "RECORD_MUSIC"),
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                }
            } else {
                Toast.makeText(
                    requireContext(),
                    getString(R.string.only_support_android_above, 10),
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
        binding.buttonBluetooth.setOnClickListener {
            needPermission(Manifest.permission.BLUETOOTH_CONNECT, !VersionUtil.S) {
                if (it) {
                    BluetoothListenerDialog().show(
                        parentFragmentManager,
                        "BluetoothListenerDialog"
                    )
                } else {
                    Toast.makeText(
                        requireContext(),
                        getString(R.string.permission_denied, "BLUETOOTH_CONNECT"),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
        binding.rawDataLabel.text = getString(R.string.bluetooth_bound)
        val bluetoothManager =
            requireContext().getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        val bluetoothAdapter = bluetoothManager.adapter
        needPermission(Manifest.permission.BLUETOOTH_CONNECT, !VersionUtil.S) { got ->
            if (got) {
                val adapter = NoDiffItemAdapter<BluetoothDevice>(
                    { it.name },
                    { it.address },
                    { R.drawable.ic_baseline_music_note_24 },
                    R.drawable.ic_baseline_music_note_24,
                    bluetoothAdapter.bondedDevices.toList()
                ) { bluetoothDevice ->
                    if (VersionUtil.Q) {
                        BluetoothClientDialog(bluetoothDevice).show(
                            parentFragmentManager,
                            "BluetoothClientDialog"
                        )
                    } else {
                        Toast.makeText(
                            requireContext(),
                            getString(R.string.only_support_android_above, 10),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
                binding.rawDataList.adapter = adapter
            } else {
                Toast.makeText(
                    requireContext(),
                    getString(R.string.permission_denied, "BLUETOOTH_CONNECT"),
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }
}