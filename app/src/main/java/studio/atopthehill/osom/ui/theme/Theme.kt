package studio.atopthehill.osom.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val EInkLightColorScheme =
        lightColorScheme(
                primary = EInkBackground, // Primary actions, buttons
                onPrimary = EInkTextPrimary, // Text on primary actions
                secondary = EInkTextTertiary, // Secondary elements, less prominent text
                onSecondary = EInkTextPrimary,
                tertiary = EInkTextTertiary, // Borders, lines
                onTertiary = EInkTextPrimary,
                background = EInkBackground, // App background
                onBackground = EInkAccent, // Main text color on background
                surface = EInkBackground, // Surfaces like cards
                onSurface = EInkTextPrimary, // Text on surfaces
                error = EInkError, // Standard error color, can be adapted
                onError = Color.White
                // Other colors can be defined as needed or left to defaults
                )

// Example for a dark E-Ink theme, if we were to implement it fully
private val EInkDarkColorScheme =
        darkColorScheme(
                primary = EInkTextPrimary, // Light text/icons on dark buttons
                onPrimary = EInkBackground,
                secondary = EInkTextPrimary,
                onSecondary = EInkBackground,
                tertiary = EInkTextPrimary,
                onTertiary = EInkBackground,
                background = EInkTextPrimary, // Dark background
                onBackground = EInkBackground, // Light text on dark background
                surface = EInkTextPrimary, // Dark surfaces
                onSurface = EInkBackground, // Light text on dark surfaces
                error = EInkError,
                onError = EInkTextSecondary
        )

@Composable
fun OSOMTheme(
        darkTheme: Boolean = isSystemInDarkTheme(),

        // Dynamic color is available on Android 12+ but we'll override with E-Ink
        dynamicColor: Boolean = false, // Set to false to enforce our E-Ink theme
        content: @Composable () -> Unit
) {
        val colorScheme =
                when {
                        // For now, always use EInkLightColorScheme as per V2 rules for e-ink
                        // display
                        // dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
                        //     val context = LocalContext.current
                        //     if (darkTheme) dynamicDarkColorScheme(context) else
                        // dynamicLightColorScheme(context)
                        // }
                        // darkTheme -> EInkDarkColorScheme // Uncomment if a dark e-ink theme is
                        // defined
                        // and desired
                        else -> EInkLightColorScheme
                }
        val view = LocalView.current
        if (!view.isInEditMode) {
                SideEffect {
                        val window = (view.context as Activity).window
                        window.statusBarColor =
                                colorScheme.background.toArgb() // Match status bar to background
                        WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars =
                                !darkTheme // Adjust status bar icons
                }
        }

        MaterialTheme(
                colorScheme = colorScheme,
                typography = AppTypography, // Use our Fraunces typography
                content = content
        )
}
