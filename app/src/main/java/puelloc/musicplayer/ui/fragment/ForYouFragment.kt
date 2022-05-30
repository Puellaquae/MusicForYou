package puelloc.musicplayer.ui.fragment

import android.Manifest
import android.app.Activity
import android.app.Application
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import puelloc.musicplayer.BuildConfig
import puelloc.musicplayer.R
import puelloc.musicplayer.adapter.NoDiffItemAdapter
import puelloc.musicplayer.databinding.FragmentForYouBinding
import puelloc.musicplayer.ui.dialog.NFCDialog
import puelloc.musicplayer.ui.dialog.PlaybackCaptureDialog
import puelloc.musicplayer.utils.VersionUtil
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

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentForYouBinding.inflate(inflater, container, false)
        playbackQueueViewModel =
            PlaybackQueueViewModel.getInstance(requireContext().applicationContext as Application)
        return binding.root
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    inner class MyLifecycleObserver: DefaultLifecycleObserver {
        lateinit var getResult: ActivityResultLauncher<Intent>
        override fun onCreate(owner: LifecycleOwner) {
            getResult = requireActivity().activityResultRegistry.register(
                "key",
                owner,
                ActivityResultContracts.StartActivityForResult()
            ) {
                if (it.resultCode == Activity.RESULT_OK) {
                    PlaybackCaptureDialog(it.data!!).show(
                        parentFragmentManager,
                        "Playback Capture Dialog"
                    )
                } else {
                    Toast.makeText(
                        requireContext(),
                        "AudioCapture Denied!",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    lateinit var observer: MyLifecycleObserver

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (VersionUtil.Q) {
            observer = MyLifecycleObserver()
        }
        lifecycle.addObserver(observer)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.textView.text =
            getString(R.string.build_time, Date(BuildConfig.BUILD_TIME.toLong()).toString())
        binding.button.setOnClickListener {
            NFCDialog().show(parentFragmentManager, "NFC Dialog")
        }
        binding.buttonCapture.setOnClickListener {
            if (VersionUtil.Q) {
                if (ContextCompat.checkSelfPermission(
                        requireContext(),
                        Manifest.permission.RECORD_AUDIO
                    ) == PackageManager.PERMISSION_GRANTED
                ) {

                    val mediaProjectionManager =
                        requireContext().applicationContext.getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
                    observer.getResult.launch(mediaProjectionManager.createScreenCaptureIntent())
                } else {
                    requireActivity().registerForActivityResult(ActivityResultContracts.RequestPermission()) {
                        if (!it) {
                            Toast.makeText(
                                requireContext(),
                                "Permission Denied!",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }.launch(Manifest.permission.RECORD_AUDIO)
                }
            }
        }
        binding.rawDataLabel.text = "BluetoothBound"
        val bluetoothManager =
            requireContext().getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        val bluetoothAdapter = bluetoothManager.adapter
        if (ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.BLUETOOTH_CONNECT
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            val adapter = NoDiffItemAdapter<BluetoothDevice>(
                { it.name },
                { it.address },
                { R.drawable.ic_baseline_music_note_24 },
                R.drawable.ic_baseline_music_note_24,
                bluetoothAdapter.bondedDevices.toList()
            ) {

            }
            binding.rawDataList.adapter = adapter
        }
    }
}