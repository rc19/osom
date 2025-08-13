package studio.atopthehill.osom.ui.permissions

import android.app.Activity
import android.content.Context
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import studio.atopthehill.osom.utils.PermissionManager

@Composable
fun PermissionScreen(onPermissionsGranted: () -> Unit) {
    val context = LocalContext.current
    var permissionStep by remember { mutableStateOf(0) }

    val requestUsageStatsPermission = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
        onResult = { permissionStep++ }
    )

    val requestAccessibilityPermission = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
        onResult = { permissionStep++ }
    )

    val requestOverlayPermission = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
        onResult = { permissionStep++ }
    )

    val requestNotificationListenerPermission = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
        onResult = {
            PermissionManager.setNotificationListenerState(context, true)
            permissionStep++
        }
    )

    val permissions = listOf(
        PermissionRequest(
            name = "Usage Stats",
            description = "This permission is required to track app usage.",
            hasPermission = { PermissionManager.hasUsageStatsPermission(context) },
            requestPermission = { requestUsageStatsPermission.launch(Intent(android.provider.Settings.ACTION_USAGE_ACCESS_SETTINGS)) }
        ),
        PermissionRequest(
            name = "Accessibility",
            description = "This permission is required to read screen content.",
            hasPermission = { PermissionManager.hasAccessibilityPermission(context) },
            requestPermission = { requestAccessibilityPermission.launch(Intent(android.provider.Settings.ACTION_ACCESSIBILITY_SETTINGS)) }
        ),
        PermissionRequest(
            name = "Overlay",
            description = "This permission is required to draw over other apps.",
            hasPermission = { PermissionManager.hasOverlayPermission(context) },
            requestPermission = { requestOverlayPermission.launch(Intent(android.provider.Settings.ACTION_MANAGE_OVERLAY_PERMISSION)) }
        ),
        PermissionRequest(
            name = "Notification Listener",
            description = "This permission is required to read notifications.",
            hasPermission = { PermissionManager.hasNotificationListenerPermission(context) },
            requestPermission = { requestNotificationListenerPermission.launch(Intent(android.provider.Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)) }
        )
    )

    LaunchedEffect(permissionStep) {
        if (permissionStep >= permissions.size) {
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
        if (permissionStep < permissions.size) {
            val permission = permissions[permissionStep]
            if (!permission.hasPermission()) {
                PermissionRequestUI(
                    permissionName = permission.name,
                    description = permission.description,
                    onClick = permission.requestPermission
                )
            } else {
                LaunchedEffect(Unit) {
                    permissionStep++
                }
            }
        }
    }
}

data class PermissionRequest(
    val name: String,
    val description: String,
    val hasPermission: () -> Boolean,
    val requestPermission: () -> Unit
)

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
