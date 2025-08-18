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
import studio.atopthehill.osom.ui.today.TodayScreen
import studio.atopthehill.osom.ui.today.TodayViewModel
import studio.atopthehill.osom.ui.today.TodayViewModelFactory
import studio.atopthehill.osom.ui.addtask.AddTaskScreen
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
    val application = context.applicationContext as OsomApplication

    val launcherViewModel: studio.atopthehill.osom.ui.launcher.LauncherViewModel = viewModel(
        factory = studio.atopthehill.osom.ui.launcher.LauncherViewModelFactory(application)
    )
    val todayViewModel: TodayViewModel = viewModel(
        factory = TodayViewModelFactory(application)
    )
    val showOnboarding by launcherViewModel.showOnboarding.collectAsStateWithLifecycle()

    val startDestination = if (showOnboarding) {
        Screen.Launcher.route
    } else {
        if (PermissionManager.hasUsageStatsPermission(context) &&
            PermissionManager.hasAccessibilityPermission(context) &&
            PermissionManager.hasOverlayPermission(context) &&
            PermissionManager.hasNotificationListenerPermission(context)) {
            Screen.Today.route
        } else {
            Screen.Permissions.route
        }
    }

    NavHost(navController = navController, startDestination = startDestination) {
        composable(Screen.Today.route) {
            val pendingTasks by todayViewModel.pendingTasks.collectAsStateWithLifecycle()
            val completedTasks by todayViewModel.completedTasks.collectAsStateWithLifecycle()
            TodayScreen(
                pendingTasks = pendingTasks,
                completedTasks = completedTasks,
                onAddTask = { navController.navigate(Screen.AddTask.route) },
                onTaskCompleted = { task -> todayViewModel.onTaskCompleted(task) },
                onTaskSnoozed = { task -> todayViewModel.onTaskSnoozed(task) },
                onTaskDismissed = { task -> todayViewModel.onTaskDismissed(task) }
            )
        }
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
                        navController.navigate(Screen.Today.route) {
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
                navController.navigate(Screen.Today.route) {
                    popUpTo(Screen.Permissions.route) {
                        inclusive = true
                    }
                }
            })
        }
        composable(Screen.AddTask.route) {
            AddTaskScreen(navController = navController)
        }
    }
}
