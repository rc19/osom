package studio.atopthehill.osom.ui.navigation

sealed class Screen(val route: String) {
    data object Launcher : Screen("launcher_screen")
    data object Summary : Screen("summary_screen")
    data object AppList : Screen("app_list_screen")
}
