
package studio.atopthehill.osom.ui.permissions

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import studio.atopthehill.osom.utils.PermissionManager

@Composable
fun PermissionScreen(onPermissionsGranted: () -> Unit) {
    val context = LocalContext.current

    val hasUsageStats = PermissionManager.hasUsageStatsPermission(context)
    val hasAccessibility = PermissionManager.hasAccessibilityPermission(context)
    val hasOverlay = PermissionManager.hasOverlayPermission(context)
    val hasNotificationListener = PermissionManager.hasNotificationListenerPermission(context)

    LaunchedEffect(hasUsageStats, hasAccessibility, hasOverlay, hasNotificationListener) {
        if (hasUsageStats && hasAccessibility && hasOverlay && hasNotificationListener) {
            onPermissionsGranted()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Permissions Required",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        if (!hasUsageStats) {
            PermissionRequestUI(
                permissionName = "Usage Stats",
                description = "This permission is required to track app usage.",
                onClick = { PermissionManager.requestUsageStatsPermission(context as android.app.Activity) }
            )
        }

        if (!hasAccessibility) {
            PermissionRequestUI(
                permissionName = "Accessibility",
                description = "This permission is required to read screen content.",
                onClick = { PermissionManager.requestAccessibilityPermission(context as android.app.Activity) }
            )
        }

        if (!hasOverlay) {
            PermissionRequestUI(
                permissionName = "Overlay",
                description = "This permission is required to draw over other apps.",
                onClick = { PermissionManager.requestOverlayPermission(context as android.app.Activity) }
            )
        }

        if (!hasNotificationListener) {
            PermissionRequestUI(
                permissionName = "Notification Listener",
                description = "This permission is required to read notifications.",
                onClick = { PermissionManager.requestNotificationListenerPermission(context as android.app.Activity) }
            )
        }
    }
}

@Composable
fun PermissionRequestUI(permissionName: String, description: String, onClick: () -> Unit) {
    Column(modifier = Modifier.padding(vertical = 8.dp)) {
        Text(text = permissionName, style = MaterialTheme.typography.titleMedium)
        Text(text = description, style = MaterialTheme.typography.bodyMedium)
        Button(onClick = onClick, modifier = Modifier.padding(top = 8.dp)) {
            Text(text = "Grant Permission")
        }
    }
}

