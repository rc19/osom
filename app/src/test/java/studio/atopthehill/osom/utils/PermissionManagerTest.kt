package studio.atopthehill.osom.utils

import android.app.Application
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowAppOpsManager
import org.robolectric.shadows.ShadowApplication
import android.provider.Settings

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.Q])
class PermissionManagerTest {

    private lateinit var context: Context
    private lateinit var shadowApp: ShadowApplication

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        shadowApp = Shadows.shadowOf(context as Application)
    }

    @Test
    fun `checkAndRequestPermissions requests permissions that are not granted`() {
        shadowApp.grantPermissions(PermissionManager.REQUIRED_PERMISSIONS[0])

        val activity = Robolectric.buildActivity(android.app.Activity::class.java).get()
        PermissionManager.checkAndRequestPermissions(activity)

        val lastRequest = Shadows.shadowOf(activity).lastRequestedPermission
        val requestedPermissions = lastRequest.requestedPermissions
        assertTrue(requestedPermissions.contains(PermissionManager.REQUIRED_PERMISSIONS[1]))
        assertFalse(requestedPermissions.contains(PermissionManager.REQUIRED_PERMISSIONS[0]))
    }

    @Test
    fun `hasUsageStatsPermission returns true when permission is granted`() {
        val appOpsManager = context.getSystemService(Context.APP_OPS_SERVICE) as android.app.AppOpsManager
        val shadowAppOpsManager = Shadows.shadowOf(appOpsManager) as ShadowAppOpsManager
        shadowAppOpsManager.setMode(
            android.app.AppOpsManager.OPSTR_GET_USAGE_STATS,
            android.os.Process.myUid(),
            context.packageName,
            android.app.AppOpsManager.MODE_ALLOWED
        )

        assertTrue(PermissionManager.hasUsageStatsPermission(context))
    }

    @Test
    fun `hasAccessibilityPermission returns true when service is enabled`() {
        Settings.Secure.putString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,
            "studio.atopthehill.osom/studio.atopthehill.osom.services.OsomAccessibilityService"
        )
        Settings.Secure.putInt(
            context.contentResolver,
            Settings.Secure.ACCESSIBILITY_ENABLED,
            1
        )

        assertTrue(PermissionManager.hasAccessibilityPermission(context))
    }

    @Test
    fun `hasOverlayPermission returns true when permission is granted`() {
        org.robolectric.shadows.ShadowSettings.setCanDrawOverlays(true)
        assertTrue(PermissionManager.hasOverlayPermission(context))
    }

    @Test
    fun `hasUsageStatsPermission returns false when permission is not granted`() {
        val appOpsManager = context.getSystemService(Context.APP_OPS_SERVICE) as android.app.AppOpsManager
        val shadowAppOpsManager = Shadows.shadowOf(appOpsManager) as ShadowAppOpsManager
        shadowAppOpsManager.setMode(
            android.app.AppOpsManager.OPSTR_GET_USAGE_STATS,
            android.os.Process.myUid(),
            context.packageName,
            android.app.AppOpsManager.MODE_ERRORED
        )

        assertFalse(PermissionManager.hasUsageStatsPermission(context))
    }

    @Test
    fun `hasAccessibilityPermission returns false when service is disabled`() {
        Settings.Secure.putString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,
            ""
        )
        Settings.Secure.putInt(
            context.contentResolver,
            Settings.Secure.ACCESSIBILITY_ENABLED,
            0
        )

        assertFalse(PermissionManager.hasAccessibilityPermission(context))
    }

    @Test
    fun `hasOverlayPermission returns false when permission is not granted`() {
        org.robolectric.shadows.ShadowSettings.setCanDrawOverlays(false)
        assertFalse(PermissionManager.hasOverlayPermission(context))
    }

    @Test
    fun `requestAccessibilityPermission with context launches correct intent`() {
        PermissionManager.requestAccessibilityPermission(context)

        val startedIntent = shadowApp.nextStartedActivity
        assert(startedIntent.action == Settings.ACTION_ACCESSIBILITY_SETTINGS)
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.TIRAMISU])
    fun `hasNotificationPermission returns true when POST_NOTIFICATIONS is granted on Android 13+`() {
        // Grant POST_NOTIFICATIONS permission
        shadowApp.grantPermissions(android.Manifest.permission.POST_NOTIFICATIONS)
        
        assertTrue(PermissionManager.hasNotificationPermission(context))
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.TIRAMISU])
    fun `hasNotificationPermission returns false when POST_NOTIFICATIONS is not granted on Android 13+`() {
        // Don't grant POST_NOTIFICATIONS permission (default state)
        
        assertFalse(PermissionManager.hasNotificationPermission(context))
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.S])
    fun `hasNotificationPermission returns true on Android 12 and below regardless of permission state`() {
        // On Android 12 and below, notifications are allowed by default
        // Even if we don't grant the permission, it should return true
        
        assertTrue(PermissionManager.hasNotificationPermission(context))
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.Q])
    fun `hasNotificationPermission returns true on Android 10`() {
        // POST_NOTIFICATIONS permission didn't exist on Android 10
        // Should always return true
        
        assertTrue(PermissionManager.hasNotificationPermission(context))
    }

    @Test
    fun `REQUIRED_PERMISSIONS contains expected permissions`() {
        val requiredPermissions = PermissionManager.REQUIRED_PERMISSIONS
        
        assertTrue(requiredPermissions.contains(android.Manifest.permission.POST_NOTIFICATIONS))
        assertTrue(requiredPermissions.contains(android.Manifest.permission.WRITE_EXTERNAL_STORAGE))
        assertEquals(2, requiredPermissions.size)
    }

    @Test
    fun `requestUsageStatsPermission launches correct intent`() {
        val activity = Robolectric.buildActivity(android.app.Activity::class.java).get()
        PermissionManager.requestUsageStatsPermission(activity)

        val startedIntent = shadowApp.nextStartedActivity
        assert(startedIntent.action == android.provider.Settings.ACTION_USAGE_ACCESS_SETTINGS)
    }

    @Test
    fun `requestOverlayPermission launches correct intent`() {
        val activity = Robolectric.buildActivity(android.app.Activity::class.java).get()
        PermissionManager.requestOverlayPermission(activity)

        val startedIntent = shadowApp.nextStartedActivity
        assert(startedIntent.action == android.provider.Settings.ACTION_MANAGE_OVERLAY_PERMISSION)
    }
}
