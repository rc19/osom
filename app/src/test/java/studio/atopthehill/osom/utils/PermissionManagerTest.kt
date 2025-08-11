package studio.atopthehill.osom.utils

import android.app.Application
import android.content.Context
import android.os.Build
import androidx.test.core.app.ApplicationProvider
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
    fun `hasNotificationListenerPermission returns true when service is enabled`() {
        val componentName = "studio.atopthehill.osom/studio.atopthehill.osom.services.OsomNotificationListenerService"
        Settings.Secure.putString(
            context.contentResolver,
            "enabled_notification_listeners",
            componentName
        )

        assertTrue(PermissionManager.hasNotificationListenerPermission(context))
    }
}
