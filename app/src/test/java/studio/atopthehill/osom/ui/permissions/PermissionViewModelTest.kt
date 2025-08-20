package studio.atopthehill.osom.ui.permissions

import android.app.Application
import android.content.Context
import android.os.Build
import android.provider.Settings
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowAppOpsManager
import org.robolectric.shadows.ShadowApplication

/**
 * Comprehensive tests for PermissionViewModel to ensure all permission logic
 * works correctly.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.Q])
class PermissionViewModelTest {

    private lateinit var viewModel: PermissionViewModel
    private lateinit var context: Context
    private lateinit var shadowApp: ShadowApplication

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        shadowApp = Shadows.shadowOf(context as Application)
        viewModel = PermissionViewModel()
    }

    // Accessibility Permission Tests
    @Test
    fun `isAccessibilityPermissionGranted returns true when service is enabled`() {
        enableAccessibilityService()
        assertTrue(viewModel.isAccessibilityPermissionGranted(context))
    }

    @Test
    fun `isAccessibilityPermissionGranted returns false when service is disabled`() {
        disableAccessibilityService()
        assertFalse(viewModel.isAccessibilityPermissionGranted(context))
    }

    // Usage Stats Permission Tests
    @Test
    fun `isUsageStatsPermissionGranted returns true when permission is granted`() {
        grantUsageStatsPermission()
        assertTrue(viewModel.isUsageStatsPermissionGranted(context))
    }

    @Test
    fun `isUsageStatsPermissionGranted returns false when permission is not granted`() {
        denyUsageStatsPermission()
        assertFalse(viewModel.isUsageStatsPermissionGranted(context))
    }

    // Overlay Permission Tests
    @Test
    fun `isOverlayPermissionGranted returns true when permission is granted`() {
        org.robolectric.shadows.ShadowSettings.setCanDrawOverlays(true)
        assertTrue(viewModel.isOverlayPermissionGranted(context))
    }

    @Test
    fun `isOverlayPermissionGranted returns false when permission is not granted`() {
        org.robolectric.shadows.ShadowSettings.setCanDrawOverlays(false)
        assertFalse(viewModel.isOverlayPermissionGranted(context))
    }

    // Notification Posting Permission Tests (Android 13+)
    @Test
    @Config(sdk = [Build.VERSION_CODES.TIRAMISU])
    fun `isNotificationPostingPermissionGranted returns true when POST_NOTIFICATIONS is granted on Android 13+`() {
        shadowApp.grantPermissions(android.Manifest.permission.POST_NOTIFICATIONS)
        assertTrue(viewModel.isNotificationPostingPermissionGranted(context))
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.TIRAMISU])
    fun `isNotificationPostingPermissionGranted returns false when POST_NOTIFICATIONS is not granted on Android 13+`() {
        // Don't grant permission (default state)
        assertFalse(viewModel.isNotificationPostingPermissionGranted(context))
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.S])
    fun `isNotificationPostingPermissionGranted returns true on Android 12 and below`() {
        // Should return true regardless of permission state on older Android versions
        assertTrue(viewModel.isNotificationPostingPermissionGranted(context))
    }

    // Combined Permission Tests
    @Test
    fun `areAllPermissionsGranted returns true when all permissions are granted`() {
        grantAllPermissions()
        assertTrue(viewModel.areAllPermissionsGranted(context))
    }

    @Test
    fun `areAllPermissionsGranted returns false when accessibility permission is missing`() {
        grantAllPermissions()
        disableAccessibilityService() // Remove one permission
        assertFalse(viewModel.areAllPermissionsGranted(context))
    }

    @Test
    fun `areAllPermissionsGranted returns false when usage stats permission is missing`() {
        grantAllPermissions()
        denyUsageStatsPermission() // Remove one permission
        assertFalse(viewModel.areAllPermissionsGranted(context))
    }

    @Test
    fun `areAllPermissionsGranted returns false when overlay permission is missing`() {
        grantAllPermissions()
        org.robolectric.shadows.ShadowSettings.setCanDrawOverlays(false) // Remove one permission
        assertFalse(viewModel.areAllPermissionsGranted(context))
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.TIRAMISU])
    fun `areAllPermissionsGranted returns false when notification permission is missing on Android 13+`() {
        grantAllPermissions()
        // Don't grant POST_NOTIFICATIONS (shadowApp doesn't grant it by default)
        shadowApp.denyPermissions(android.Manifest.permission.POST_NOTIFICATIONS)
        assertFalse(viewModel.areAllPermissionsGranted(context))
    }

    // Missing Permissions Tests
    @Test
    fun `getMissingPermissions returns empty list when all permissions are granted`() {
        grantAllPermissions()
        val missingPermissions = viewModel.getMissingPermissions(context)
        assertTrue("Expected no missing permissions, but found: $missingPermissions", 
                  missingPermissions.isEmpty())
    }

    @Test
    fun `getMissingPermissions returns correct list when some permissions are missing`() {
        // Grant only accessibility, leave others denied
        enableAccessibilityService()
        disableOverlayPermission()
        denyUsageStatsPermission()
        
        val missingPermissions = viewModel.getMissingPermissions(context)
        
        assertFalse("Accessibility Service should not be in missing list", 
                   missingPermissions.contains("Accessibility Service"))
        assertTrue("Usage Stats should be in missing list", 
                  missingPermissions.contains("Usage Stats"))
        assertTrue("Display over other apps should be in missing list", 
                  missingPermissions.contains("Display over other apps"))
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.TIRAMISU])
    fun `getMissingPermissions includes notification permission on Android 13+ when not granted`() {
        grantAllPermissions()
        shadowApp.denyPermissions(android.Manifest.permission.POST_NOTIFICATIONS)
        
        val missingPermissions = viewModel.getMissingPermissions(context)
        
        assertTrue("Show notifications should be in missing list on Android 13+", 
                  missingPermissions.contains("Show notifications"))
    }

    @Test
    fun `getMissingPermissions returns all permissions when none are granted`() {
        denyAllPermissions()
        
        val missingPermissions = viewModel.getMissingPermissions(context)
        
        // On Android Q (API 29), notification permissions are granted by default
        // so we expect 3 missing permissions, not 4
        assertEquals("Should have 3 missing permissions on Android Q", 3, missingPermissions.size)
        assertTrue(missingPermissions.contains("Accessibility Service"))
        assertTrue(missingPermissions.contains("Usage Stats"))
        assertTrue(missingPermissions.contains("Display over other apps"))
        // Note: "Show notifications" is not in the list because it's granted by default on Android Q
    }

    // Edge Cases and Error Scenarios
    @Test
    fun `permissions work correctly with null settings values`() {
        // Clear all settings to simulate edge case
        Settings.Secure.putString(context.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES, null)
        Settings.Secure.putInt(context.contentResolver, Settings.Secure.ACCESSIBILITY_ENABLED, 0)
        
        // Should handle null gracefully
        assertFalse(viewModel.isAccessibilityPermissionGranted(context))
    }

    // Helper Methods
    private fun enableAccessibilityService() {
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
    }

    private fun disableAccessibilityService() {
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
    }

    private fun grantUsageStatsPermission() {
        val appOpsManager = context.getSystemService(Context.APP_OPS_SERVICE) as android.app.AppOpsManager
        val shadowAppOpsManager = Shadows.shadowOf(appOpsManager) as ShadowAppOpsManager
        shadowAppOpsManager.setMode(
            android.app.AppOpsManager.OPSTR_GET_USAGE_STATS,
            android.os.Process.myUid(),
            context.packageName,
            android.app.AppOpsManager.MODE_ALLOWED
        )
    }

    private fun denyUsageStatsPermission() {
        val appOpsManager = context.getSystemService(Context.APP_OPS_SERVICE) as android.app.AppOpsManager
        val shadowAppOpsManager = Shadows.shadowOf(appOpsManager) as ShadowAppOpsManager
        shadowAppOpsManager.setMode(
            android.app.AppOpsManager.OPSTR_GET_USAGE_STATS,
            android.os.Process.myUid(),
            context.packageName,
            android.app.AppOpsManager.MODE_ERRORED
        )
    }

    private fun enableOverlayPermission() {
        org.robolectric.shadows.ShadowSettings.setCanDrawOverlays(true)
    }

    private fun disableOverlayPermission() {
        org.robolectric.shadows.ShadowSettings.setCanDrawOverlays(false)
    }

    private fun grantAllPermissions() {
        enableAccessibilityService()
        grantUsageStatsPermission()
        enableOverlayPermission()
        // POST_NOTIFICATIONS is granted by default on older Android versions
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            shadowApp.grantPermissions(android.Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    private fun denyAllPermissions() {
        disableAccessibilityService()
        denyUsageStatsPermission()
        disableOverlayPermission()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            shadowApp.denyPermissions(android.Manifest.permission.POST_NOTIFICATIONS)
        }
    }
}