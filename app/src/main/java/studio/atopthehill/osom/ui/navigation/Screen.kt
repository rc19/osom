package studio.atopthehill.osom.ui.navigation

sealed class Screen(val route: String) {
    object Today : Screen("today")
    object Launcher : Screen("launcher")
    object AppList : Screen("appList")
    object Summary : Screen("summary")
    object Permissions : Screen("permissions")
    object AddTask : Screen("addTask")
}
