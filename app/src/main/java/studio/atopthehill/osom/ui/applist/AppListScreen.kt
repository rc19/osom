package studio.atopthehill.osom.ui.applist

import android.graphics.drawable.Drawable
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import studio.atopthehill.osom.OsomApplication
import studio.atopthehill.osom.data.db.entity.AppInfo
import studio.atopthehill.osom.ui.launcher.LauncherViewModel
import studio.atopthehill.osom.ui.launcher.LauncherViewModelFactory
import studio.atopthehill.osom.ui.theme.EInkTextPrimary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppListScreen(
        navController: NavController,
        launcherViewModel: LauncherViewModel =
                viewModel(
                        factory =
                                LauncherViewModelFactory(
                                        LocalContext.current.applicationContext as OsomApplication
                                )
                )
) {
    val installedApps by launcherViewModel.allInstalledApps.collectAsState()
    val context = LocalContext.current

    Scaffold(
            topBar = {
                TopAppBar(
                        title = { Text("Installed Applications") },
                        navigationIcon = {
                            IconButton(onClick = { navController.popBackStack() }) {
                                Icon(
                                        Icons.AutoMirrored.Filled.ArrowBack,
                                        contentDescription = "Back"
                                )
                            }
                        }
                )
            }
    ) { paddingValues ->
        if (installedApps.isEmpty()) {
            Box(
                    modifier = Modifier.fillMaxSize().padding(paddingValues).padding(16.dp),
                    contentAlignment = Alignment.Center
            ) { Text("No applications found or list is loading...") }
        } else {
            LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(paddingValues),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(installedApps, key = { app -> app.packageName }) { appInfo ->
                    AppListItem(appInfo = appInfo, context = context)
                }
            }
        }
    }
}

@Composable
fun AppListItem(appInfo: AppInfo, context: android.content.Context) {
    val appIcon: Drawable? =
            remember(appInfo.packageName) {
                try {
                    context.packageManager.getApplicationIcon(appInfo.packageName)
                } catch (e: Exception) {
                    null // Handle cases where icon might not be found
                }
            }

    Card(
            modifier =
                    Modifier.fillMaxWidth()
                            .border(
                                    BorderStroke(1.dp, EInkTextPrimary),
                                    shape = RoundedCornerShape(8.dp)
                            ),
            shape = RoundedCornerShape(8.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
                modifier = Modifier.padding(12.dp).fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (appIcon != null) {
                Image(
                        painter = rememberAsyncImagePainter(model = appIcon),
                        contentDescription = "${appInfo.label} icon",
                        modifier = Modifier.size(40.dp)
                )
            } else {
                Spacer(modifier = Modifier.size(40.dp)) // Placeholder if no icon
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                        text = appInfo.label,
                        style = MaterialTheme.typography.titleMedium,
                        color = EInkTextPrimary
                )
                Text(
                        text = appInfo.packageName,
                        style = MaterialTheme.typography.bodySmall,
                        color = EInkTextPrimary
                )
            }
        }
    }
}
