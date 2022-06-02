package puelloc.musicplayer.utils

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment

class PermissionUtil {
    companion object {
        fun Fragment.requirePermission(permission: String, onResult: ((res: Boolean) -> Unit)) {
            requireActivity()
                .registerForActivityResult(
                    ActivityResultContracts.RequestPermission(),
                    onResult
                ).launch(permission)
        }

        fun Fragment.hasPermission(permission: String): Boolean {
            return ContextCompat.checkSelfPermission(
                requireContext(),
                permission
            ) == PackageManager.PERMISSION_GRANTED
        }

        fun Fragment.needPermission(
            permission: String,
            skipCheck: Boolean = false,
            onResult: (res: Boolean) -> Unit
        ) {
            if (skipCheck || hasPermission(permission)) {
                onResult(true)
            } else {
                requirePermission(permission, onResult)
            }
        }
    }
}