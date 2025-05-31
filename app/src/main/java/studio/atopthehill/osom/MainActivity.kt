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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import studio.atopthehill.osom.ui.applist.AppListScreen
import studio.atopthehill.osom.ui.launcher.LauncherScreen
import studio.atopthehill.osom.ui.navigation.Screen
import studio.atopthehill.osom.ui.summary.SummaryScreen
import studio.atopthehill.osom.ui.theme.OSOMTheme

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
                ) { OsomAppNavigation() }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Trigger usage stats logging when the activity resumes - REMOVED
        // UsageStatsLogger.logRecentUsageStats(this)
    }
}

@Composable
fun OsomAppNavigation() {
    val navController = rememberNavController()
    val launcherViewModel: studio.atopthehill.osom.ui.launcher.LauncherViewModel =
            androidx.lifecycle.viewmodel.compose.viewModel(
                    factory =
                            studio.atopthehill.osom.ui.launcher.LauncherViewModelFactory(
                                    LocalContext.current.applicationContext as OsomApplication
                            )
            )
    val navigateToRoute: String? by launcherViewModel.navigateToRoute.collectAsStateWithLifecycle()

    LaunchedEffect(navigateToRoute) {
        navigateToRoute?.let { route ->
            navController.navigate(route)
            launcherViewModel.onNavigationComplete() // Reset after navigation
        }
    }

    NavHost(navController = navController, startDestination = Screen.Launcher.route) {
        composable(Screen.Launcher.route) {
            LauncherScreen(navController = navController, launcherViewModel = launcherViewModel)
        }
        composable(Screen.Summary.route) {
            SummaryScreen(navController = navController, launcherViewModel = launcherViewModel)
        }
        composable(Screen.AppList.route) {
            AppListScreen(navController = navController, launcherViewModel = launcherViewModel)
        }
    }
}
