package studio.atopthehill.osom.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import studio.atopthehill.osom.R

// VariableFontDimension.kt
object DisplayVFConfig {
        const val WEIGHT = 600
        const val ITALICS = 0F
        const val SOFT = 500
        const val OPTICAL_SIZE = 144
}

// VariableFontDimension.kt
object BodyVFConfig {
        const val WEIGHT = 360
        const val ITALICS = 0F
        const val SOFT = 500
        const val OPTICAL_SIZE = 144
}

// VariableFontDimension.kt
object LabelVFConfig {
        const val WEIGHT = 200
        const val ITALICS = 1.0F
        const val SOFT = 500
        const val OPTICAL_SIZE = 144
}

@OptIn(ExperimentalTextApi::class)
val displayFontFamily =
        FontFamily(
                Font(
                        R.font.fraunces_variable,
                        variationSettings =
                                FontVariation.Settings(
                                        FontVariation.weight(DisplayVFConfig.WEIGHT),
                                        FontVariation.italic(DisplayVFConfig.ITALICS),
                                )
                )
        )

@OptIn(ExperimentalTextApi::class)
val bodyFontFamily =
        FontFamily(
                Font(
                        R.font.fraunces_variable,
                        variationSettings =
                                FontVariation.Settings(
                                        FontVariation.weight(BodyVFConfig.WEIGHT),
                                        FontVariation.italic(BodyVFConfig.ITALICS),
                                )
                )
        )

@OptIn(ExperimentalTextApi::class)
val labelFontFamily =
        FontFamily(
                Font(
                        R.font.fraunces_variable,
                        variationSettings =
                                FontVariation.Settings(
                                        FontVariation.weight(LabelVFConfig.WEIGHT),
                                        FontVariation.italic(LabelVFConfig.ITALICS),
                                )
                )
        )

// Define Fraunces FontFamily
val FrauncesFontFamily =
        FontFamily(
                Font(R.font.fraunces_soft_regular, FontWeight.Normal),
                Font(R.font.fraunces_light_italic, FontWeight.Light, FontStyle.Italic),
                Font(R.font.fraunces_supersoft_bold, FontWeight.Bold),
                // You can add specific weights if your variable font supports them and you need to
                // call them out
                // e.g., Font(R.font.fraunces_variable, FontWeight.Bold, FontStyle.Normal) for bold
                )

// Replace with your app's specific typography
// Set of Material typography styles to start with
val AppTypography =
        Typography(
                displayLarge =
                        TextStyle(
                                fontFamily = FrauncesFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 57.sp,
                                lineHeight = 64.sp,
                                letterSpacing = (-0.25).sp
                        ),
                displayMedium =
                        TextStyle(
                                fontFamily = FrauncesFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 45.sp,
                                lineHeight = 52.sp,
                                letterSpacing = 0.sp
                        ),
                displaySmall =
                        TextStyle(
                                fontFamily = FrauncesFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 36.sp,
                                lineHeight = 44.sp,
                                letterSpacing = 0.sp
                        ),
                headlineLarge =
                        TextStyle(
                                fontFamily = FrauncesFontFamily,
                                fontWeight =
                                        FontWeight.Bold, // Consider a bolder weight if available
                                // and desired
                                fontSize = 32.sp,
                                lineHeight = 40.sp,
                                letterSpacing = 0.sp
                        ),
                headlineMedium =
                        TextStyle(
                                fontFamily = FrauncesFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 28.sp,
                                lineHeight = 36.sp,
                                letterSpacing = 0.sp
                        ),
                headlineSmall =
                        TextStyle(
                                fontFamily = FrauncesFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 24.sp,
                                lineHeight = 32.sp,
                                letterSpacing = 0.sp
                        ),
                titleLarge =
                        TextStyle(
                                fontFamily = FrauncesFontFamily,
                                fontWeight = FontWeight.Bold, // Titles often look good bold
                                fontSize = 22.sp,
                                lineHeight = 28.sp,
                                letterSpacing = 0.sp
                        ),
                titleMedium =
                        TextStyle(
                                fontFamily = FrauncesFontFamily,
                                fontWeight = FontWeight.Normal,
                                fontSize = 16.sp,
                                lineHeight = 24.sp,
                                letterSpacing = 0.15.sp
                        ),
                titleSmall =
                        TextStyle(
                                fontFamily = FrauncesFontFamily,
                                fontWeight = FontWeight.Normal,
                                fontSize = 14.sp,
                                lineHeight = 20.sp,
                                letterSpacing = 0.1.sp
                        ),
                bodyLarge =
                        TextStyle(
                                fontFamily = FrauncesFontFamily,
                                fontWeight = FontWeight.Normal,
                                fontSize = 16.sp,
                                lineHeight = 24.sp,
                                letterSpacing = 0.5.sp
                        ),
                bodyMedium =
                        TextStyle(
                                fontFamily = FrauncesFontFamily,
                                fontWeight = FontWeight.Normal,
                                fontSize = 14.sp,
                                lineHeight = 20.sp,
                                letterSpacing = 0.25.sp
                        ),
                bodySmall =
                        TextStyle(
                                fontFamily = FrauncesFontFamily,
                                fontWeight = FontWeight.Normal,
                                fontSize = 12.sp,
                                lineHeight = 16.sp,
                                letterSpacing = 0.4.sp
                        ),
                labelLarge =
                        TextStyle(
                                fontFamily = FrauncesFontFamily, // Or a more utilitarian font if
                                // Fraunces is too decorative for labels
                                fontWeight = FontWeight.Light,
                                fontStyle = FontStyle.Italic,
                                fontSize = 14.sp,
                                lineHeight = 20.sp,
                                letterSpacing = 0.1.sp
                        ),
                labelMedium =
                        TextStyle(
                                fontFamily = FrauncesFontFamily,
                                fontWeight = FontWeight.Light,
                                fontStyle = FontStyle.Italic,
                                fontSize = 12.sp,
                                lineHeight = 16.sp,
                                letterSpacing = 0.5.sp
                        ),
                labelSmall =
                        TextStyle(
                                fontFamily = FrauncesFontFamily,
                                fontWeight = FontWeight.Light,
                                fontStyle = FontStyle.Italic,
                                fontSize = 11.sp,
                                lineHeight = 16.sp,
                                letterSpacing = 0.5.sp
                        )
        )
