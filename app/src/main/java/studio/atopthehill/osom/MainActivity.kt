package studio.atopthehill.osom

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import studio.atopthehill.osom.ui.applist.AppListScreen
import studio.atopthehill.osom.ui.launcher.LauncherScreen
import studio.atopthehill.osom.ui.navigation.Screen
import studio.atopthehill.osom.ui.permissions.PermissionScreen
import studio.atopthehill.osom.ui.summary.SummaryScreen
import studio.atopthehill.osom.ui.theme.OSOMTheme
import studio.atopthehill.osom.utils.PermissionManager

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge(
            statusBarStyle =
            SystemBarStyle.light(
                scrim = Color.Transparent.toArgb(),
                darkScrim = Color.Transparent.toArgb()
            ),
            navigationBarStyle =
            SystemBarStyle.light(
                scrim = Color.Transparent.toArgb(),
                darkScrim = Color.Transparent.toArgb()
            ),
        )
        super.onCreate(savedInstanceState)

        setContent {
            OSOMTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) { OsomApp() }
            }
        }
    }
}

@Composable
fun OsomApp() {
    val navController = rememberNavController()
    val context = LocalContext.current
    val launcherViewModel: studio.atopthehill.osom.ui.launcher.LauncherViewModel = viewModel(
        factory = studio.atopthehill.osom.ui.launcher.LauncherViewModelFactory(
            (context.applicationContext as OsomApplication)
        )
    )
    val showOnboarding by launcherViewModel.showOnboarding.collectAsStateWithLifecycle()

    val startDestination = if (showOnboarding) {
        Screen.Launcher.route
    } else {
        // If onboarding is already done, we decide the start destination based on permissions.
        if (PermissionManager.hasUsageStatsPermission(context) &&
            PermissionManager.hasAccessibilityPermission(context) &&
            PermissionManager.hasOverlayPermission(context) &&
            PermissionManager.hasNotificationListenerPermission(context)) {
            Screen.Launcher.route
        } else {
            Screen.Permissions.route
        }
    }

    NavHost(navController = navController, startDestination = startDestination) {
        composable(Screen.Launcher.route) {
            LauncherScreen(
                navController = navController,
                launcherViewModel = launcherViewModel,
                onOnboardingComplete = {
                    launcherViewModel.onOnboardingComplete()
                    if (PermissionManager.hasUsageStatsPermission(context) &&
                        PermissionManager.hasAccessibilityPermission(context) &&
                        PermissionManager.hasOverlayPermission(context) &&
                        PermissionManager.hasNotificationListenerPermission(context)) {
                        navController.navigate(Screen.Launcher.route) {
                            popUpTo(Screen.Launcher.route) { inclusive = true }
                        }
                    } else {
                        navController.navigate(Screen.Permissions.route) {
                            popUpTo(Screen.Launcher.route) { inclusive = true }
                        }
                    }
                }
            )
        }
        composable(Screen.Summary.route) {
            SummaryScreen(navController = navController, launcherViewModel = launcherViewModel)
        }
        composable(Screen.AppList.route) {
            AppListScreen(navController = navController, launcherViewModel = launcherViewModel)
        }
        composable(Screen.Permissions.route) {
            PermissionScreen(navigateToNextScreen = {
                navController.navigate(Screen.Launcher.route) {
                    popUpTo(Screen.Permissions.route) {
                        inclusive = true
                    }
                }
            })
        }
    }
}
