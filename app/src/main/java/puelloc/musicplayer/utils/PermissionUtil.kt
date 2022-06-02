package puelloc.musicplayer.utils

import android.content.pm.PackageManager
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment

class PermissionUtil {
    companion object {
        fun Fragment.requirePermissionResult(onResult: ((res: Boolean) -> Unit)): ActivityResultLauncher<String> {
            return registerForActivityResult(
                ActivityResultContracts.RequestPermission(),
                onResult
            )
        }

        fun Fragment.hasPermission(permission: String): Boolean {
            return ContextCompat.checkSelfPermission(
                requireContext(),
                permission
            ) == PackageManager.PERMISSION_GRANTED
        }
    }
}