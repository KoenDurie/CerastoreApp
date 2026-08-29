package be.kdr.agvalarm.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

val Charcoal = Color(0xFF121212)
val CharcoalSurface = Color(0xFF1C1C1C)
val CharcoalElevated = Color(0xFF262626)
val Amber = Color(0xFFFFB300)
val AmberDim = Color(0xFFE6A100)
val AlarmRed = Color(0xFFFF6E40)
val OfflineGray = Color(0xFF9E9E9E)
val ConnectedGreen = Color(0xFF69F0AE)
val OnCharcoal = Color(0xFFF5F0E6)

private val DarkColors = darkColorScheme(
    primary = Amber,
    onPrimary = Color(0xFF1A1400),
    secondary = AmberDim,
    background = Charcoal,
    surface = CharcoalSurface,
    surfaceVariant = CharcoalElevated,
    onBackground = OnCharcoal,
    onSurface = OnCharcoal,
    onSurfaceVariant = Color(0xFFD7D0C4),
    error = AlarmRed,
    onError = Color.White,
    outline = Color(0xFF5C5346),
)

private val Typography = Typography(
    displaySmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 32.sp,
        lineHeight = 38.sp,
    ),
    headlineMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 24.sp,
        lineHeight = 30.sp,
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp,
        lineHeight = 26.sp,
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 18.sp,
        lineHeight = 24.sp,
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontSize = 16.sp,
        lineHeight = 22.sp,
    ),
    labelLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        letterSpacing = 0.4.sp,
    ),
)

@Composable
fun AgvAlarmTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColors,
        typography = Typography,
        content = content,
    )
}
