package studio.atopthehill.osom.ui.controlcenter

import android.app.Application
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import studio.atopthehill.osom.OsomApplication
import studio.atopthehill.osom.data.db.entity.AppInfo

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ControlCenterScreen() {
    val context = LocalContext.current
    val application = context.applicationContext as OsomApplication
    val viewModel: ControlCenterViewModel = viewModel(
        factory = ControlCenterViewModelFactory(application)
    )
    val apps by viewModel.allApps.collectAsState()
    val userStats by viewModel.userStats.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Control Center") })
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            Text(
                text = "Read and monitor ONLY These Apps",
                style = MaterialTheme.typography.titleLarge
            )
            Text(
                text = "Osom will only observe any on-screen content from selected apps.",
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(modifier = Modifier.height(16.dp))
            LazyColumn(
                modifier = Modifier.weight(1f)
            ) {
                items(apps) { app ->
                    AppListItem(app = app, onToggle = { packageName, isWhitelisted ->
                        viewModel.setAppWhitelisted(packageName, isWhitelisted)
                    })
                }
            }
            Divider(modifier = Modifier.padding(vertical = 16.dp))
            NotificationPreferences(
                userStats = userStats,
                onActiveRemindersChanged = { viewModel.setActiveReminders(it) },
                onAITasksEnabledChanged = { viewModel.setAITasksEnabled(it) }
            )
            Divider(modifier = Modifier.padding(vertical = 16.dp))
            ManagePermissions()
        }
    }
}

@Composable
fun AppListItem(app: AppInfo, onToggle: (String, Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val bitmap = app.icon?.let {
            BitmapFactory.decodeByteArray(it, 0, it.size)
        }
        if (bitmap != null) {
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = null,
                modifier = Modifier.size(40.dp)
            )
        }
        Spacer(modifier = Modifier.size(16.dp))
        Text(text = app.label, modifier = Modifier.weight(1f))
        Switch(
            checked = app.isWhitelisted,
            onCheckedChange = { onToggle(app.packageName, it) }
        )
    }
}

@Composable
fun NotificationPreferences(
    userStats: studio.atopthehill.osom.data.db.entity.UserStats?, 
    onActiveRemindersChanged: (Boolean) -> Unit,
    onAITasksEnabledChanged: (Boolean) -> Unit
) {
    Column {
        Text(
            text = "Smart Features",
            style = MaterialTheme.typography.titleLarge
        )
        Spacer(modifier = Modifier.height(8.dp))
        
        // AI Task Creation Toggle
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = "Smart Task Creation")
                Text(
                    text = "Analyze screen content to create meaningful tasks",
                    style = MaterialTheme.typography.bodySmall
                )
            }
            Switch(
                checked = userStats?.enableAITasks ?: false, 
                onCheckedChange = onAITasksEnabledChanged
            )
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Active Reminders Toggle
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = "Active Reminders", modifier = Modifier.weight(1f))
            Switch(checked = userStats?.activeReminders ?: true, onCheckedChange = onActiveRemindersChanged)
        }
    }
}

@Composable
fun ManagePermissions() {
    val context = LocalContext.current
    Column {
        Text(
            text = "Manage Permissions",
            style = MaterialTheme.typography.titleLarge
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Manage Permissions",
            modifier = Modifier.clickable {
                val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                context.startActivity(intent)
            }
        )
    }
}
