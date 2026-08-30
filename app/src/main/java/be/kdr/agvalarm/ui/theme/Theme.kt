package be.kdr.agvalarm.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

val SageCanvas = Color(0xFF8B9081)
val SageOlive = Color(0xFF7E8A6B)
val SageDeep = Color(0xFF6F7566)
val GlassWhite = Color(0xF5F4F2EC)
val FrostWhite = Color(0xE8FFFFFF)
val CardWhite = Color(0xFFF7F6F2)
val AccentOrange = Color(0xFFF26522)
val Lime = Color(0xFFB6E326)
val Coral = Color(0xFFFF4D4D)
val Ink = Color(0xFF111111)
val LabelGrey = Color(0xFF6B7068)
val TrackGrey = Color(0xFFD8D6CE)
val OfflineGray = Color(0xFF8A8E86)

/** Back-compat aliases used by older UI pieces. */
val Amber = AccentOrange
val AlarmRed = Coral
val ConnectedGreen = Lime
val CharcoalElevated = CardWhite

private val LightColors = lightColorScheme(
    primary = AccentOrange,
    onPrimary = Color.White,
    secondary = SageOlive,
    onSecondary = Color.White,
    background = SageCanvas,
    surface = CardWhite,
    surfaceVariant = GlassWhite,
    onBackground = Ink,
    onSurface = Ink,
    onSurfaceVariant = LabelGrey,
    error = Coral,
    onError = Color.White,
    outline = Color(0xFFC4C2B8),
    tertiary = Lime,
)

private val AppTypography = Typography(
    displaySmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 34.sp,
        lineHeight = 40.sp,
        color = Ink,
    ),
    headlineMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 26.sp,
        lineHeight = 32.sp,
        color = Ink,
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 18.sp,
        lineHeight = 24.sp,
        color = Ink,
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp,
        lineHeight = 22.sp,
        color = Ink,
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        color = LabelGrey,
    ),
    labelLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 12.sp,
        letterSpacing = 0.3.sp,
        color = LabelGrey,
    ),
    labelSmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 14.sp,
        color = LabelGrey,
    ),
)

@Composable
fun AgvAlarmTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LightColors,
        typography = AppTypography,
        content = content,
    )
}
