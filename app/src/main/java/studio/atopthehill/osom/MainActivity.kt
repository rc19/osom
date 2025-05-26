package studio.atopthehill.osom

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import studio.atopthehill.osom.ui.launcher.LauncherScreen
import studio.atopthehill.osom.ui.theme.OSOMTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            OSOMTheme {
                Surface(
                        modifier = Modifier
                        .fillMaxSize() // Adds 8dp padding on all sides.
                        .navigationBarsPadding() // adds padding to system navigation bar
                        .imePadding(),
                        color = MaterialTheme.colorScheme.background
                ) { LauncherScreen() }
            }
        }
    }
}
