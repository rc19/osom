
package studio.atopthehill.osom.ui.permissions

import android.content.Context
import androidx.lifecycle.ViewModel
import studio.atopthehill.osom.utils.PermissionManager

/**
 * ViewModel for managing all app permissions required for full functionality.
 * 
 * This class provides methods to check and request all critical permissions:
 * - Accessibility Service (for screen content monitoring)
 * - Usage Stats (for app usage tracking)
 * - System Alert Window (for overlay/reminder functionality)
 * - Notification Listener (for notification analysis)
 * - Notification Posting (for showing app update notifications)
 */
class PermissionViewModel : ViewModel() {

    /**
     * Check if accessibility permission is granted.
     * Required for monitoring screen content from whitelisted apps.
     */
    fun isAccessibilityPermissionGranted(context: Context): Boolean {
        return PermissionManager.hasAccessibilityPermission(context)
    }

    /**
     * Launch accessibility settings for user to grant permission.
     */
    fun launchAccessibilitySettings(context: Context) {
        PermissionManager.requestAccessibilityPermission(context)
    }

    /**
     * Check if usage stats permission is granted.
     * Required for tracking app usage time and generating insights.
     */
    fun isUsageStatsPermissionGranted(context: Context): Boolean {
        return PermissionManager.hasUsageStatsPermission(context)
    }

    /**
     * Launch usage access settings for user to grant permission.
     */
    fun launchUsageStatsSettings(context: Context) {
        PermissionManager.requestUsageStatsPermission(context as android.app.Activity)
    }

    /**
     * Check if overlay permission is granted.
     * Required for showing reminders and prompts over other apps.
     */
    fun isOverlayPermissionGranted(context: Context): Boolean {
        return PermissionManager.hasOverlayPermission(context)
    }

    /**
     * Launch overlay permission settings for user to grant permission.
     */
    fun launchOverlaySettings(context: Context) {
        PermissionManager.requestOverlayPermission(context as android.app.Activity)
    }

    /**
     * Check if notification posting permission is granted.
     * This is critical for app update notifications to work.
     */
    fun isNotificationPostingPermissionGranted(context: Context): Boolean {
        return if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            androidx.core.content.ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.POST_NOTIFICATIONS
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        } else {
            // Notifications are allowed by default on Android 12 and below
            true
        }
    }

    /**
     * Check if all critical permissions are granted.
     * This determines if the app can function fully.
     */
    fun areAllPermissionsGranted(context: Context): Boolean {
        return isAccessibilityPermissionGranted(context) &&
                isUsageStatsPermissionGranted(context) &&
                isOverlayPermissionGranted(context) &&
                isNotificationPostingPermissionGranted(context)
    }

    /**
     * Get a list of missing permissions with their names.
     * Useful for showing users what still needs to be granted.
     */
    fun getMissingPermissions(context: Context): List<String> {
        val missing = mutableListOf<String>()
        
        if (!isAccessibilityPermissionGranted(context)) {
            missing.add("Accessibility Service")
        }
        if (!isUsageStatsPermissionGranted(context)) {
            missing.add("Usage Stats")
        }
        if (!isOverlayPermissionGranted(context)) {
            missing.add("Display over other apps")
        }
        if (!isNotificationPostingPermissionGranted(context)) {
            missing.add("Show notifications")
        }
        
        return missing
    }
}
