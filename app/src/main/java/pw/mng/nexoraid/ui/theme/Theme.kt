package pw.mng.nexoraid.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = AccentPrimary,
    secondary = AccentSecondary,
    tertiary = AccentTertiary,
    background = TechBlack,
    surface = TechGray,
    onPrimary = TechBlack,
    onSecondary = TechText,
    onTertiary = TechText,
    onBackground = TechText,
    onSurface = TechText,
    surfaceVariant = TechBorder,
    onSurfaceVariant = TechTextMuted
)

private val LightColorScheme = lightColorScheme(
    primary = AccentPrimary,
    secondary = AccentSecondary,
    tertiary = AccentTertiary,
    background = LightBg,
    surface = LightSurface,
    onPrimary = LightSurface,
    onSecondary = LightText,
    onTertiary = LightSurface,
    onBackground = LightText,
    onSurface = LightText,
    surfaceVariant = LightBorder,
    onSurfaceVariant = TechTextMuted
)

@Composable
fun NexoRaidTheme(
    darkTheme: Boolean = true, // Forced Dark Mode as per user request
    persona: pw.mng.nexoraid.data.Persona? = null,
    content: @Composable () -> Unit
) {
    val baseScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    
    val colorScheme = if (persona != null) {
        baseScheme.copy(
            primary = androidx.compose.ui.graphics.Color(persona.primaryColor.toInt()),
            tertiary = androidx.compose.ui.graphics.Color(persona.tertiaryColor.toInt())
        )
    } else {
        baseScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}