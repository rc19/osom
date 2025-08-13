
package studio.atopthehill.osom.ui.permissions

import android.content.Context
import androidx.lifecycle.ViewModel
import studio.atopthehill.osom.utils.PermissionManager

class PermissionViewModel : ViewModel() {

    fun isAccessibilityPermissionGranted(context: Context): Boolean {
        return PermissionManager.hasAccessibilityPermission(context)
    }

    fun launchAccessibilitySettings(context: Context) {
        PermissionManager.requestAccessibilityPermission(context)
    }
}
