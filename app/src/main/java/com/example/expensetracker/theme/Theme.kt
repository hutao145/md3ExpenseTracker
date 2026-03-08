package com.example.expensetracker.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = PrimaryDark,
    onPrimary = OnPrimaryDark,
    primaryContainer = PrimaryContainerDark,
    onPrimaryContainer = OnPrimaryContainerDark,
    secondary = SecondaryDark,
    onSecondary = OnSecondaryDark,
    secondaryContainer = SecondaryContainerDark,
    onSecondaryContainer = OnSecondaryContainerDark,
    tertiary = TertiaryDark,
    onTertiary = OnTertiaryDark,
    tertiaryContainer = TertiaryContainerDark,
    onTertiaryContainer = OnTertiaryContainerDark,
    error = ErrorDark,
    onError = OnErrorDark,
    errorContainer = ErrorContainerDark,
    onErrorContainer = OnErrorContainerDark,
    background = BackgroundDark,
    onBackground = OnBackgroundDark,
    surface = SurfaceDark,
    onSurface = OnSurfaceDark,
    surfaceVariant = SurfaceVariantDark,
    onSurfaceVariant = OnSurfaceVariantDark,
    outline = OutlineDark
)

private val LightColorScheme = lightColorScheme(
    primary = Primary,
    onPrimary = OnPrimary,
    primaryContainer = PrimaryContainer,
    onPrimaryContainer = OnPrimaryContainer,
    secondary = Secondary,
    onSecondary = OnSecondary,
    secondaryContainer = SecondaryContainer,
    onSecondaryContainer = OnSecondaryContainer,
    tertiary = Tertiary,
    onTertiary = OnTertiary,
    tertiaryContainer = TertiaryContainer,
    onTertiaryContainer = OnTertiaryContainer,
    error = Error,
    onError = OnError,
    errorContainer = ErrorContainer,
    onErrorContainer = OnErrorContainer,
    background = Background,
    onBackground = OnBackground,
    surface = Surface,
    onSurface = OnSurface,
    surfaceVariant = SurfaceVariant,
    onSurfaceVariant = OnSurfaceVariant,
    outline = Outline
)

// 1. Sakura Pink (樱花粉)
private val DarkPinkColorScheme = darkColorScheme(
    primary = PrimaryDarkPink, onPrimary = OnPrimaryDarkPink, primaryContainer = PrimaryContainerDarkPink, onPrimaryContainer = OnPrimaryContainerDarkPink,
    secondary = SecondaryDarkPink, onSecondary = OnSecondaryDarkPink, secondaryContainer = SecondaryContainerDarkPink, onSecondaryContainer = OnSecondaryContainerDarkPink,
    tertiary = TertiaryDarkPink, onTertiary = OnTertiaryDarkPink, tertiaryContainer = TertiaryContainerDarkPink, onTertiaryContainer = OnTertiaryContainerDarkPink,
    error = ErrorDarkPink, onError = OnErrorDarkPink, errorContainer = ErrorContainerDarkPink, onErrorContainer = OnErrorContainerDarkPink,
    background = BackgroundDark, onBackground = OnBackgroundDark, surface = SurfaceDark, onSurface = OnSurfaceDark, surfaceVariant = SurfaceVariantDark, onSurfaceVariant = OnSurfaceVariantDark, outline = OutlineDark
)
private val LightPinkColorScheme = lightColorScheme(
    primary = PrimaryPink, onPrimary = OnPrimaryPink, primaryContainer = PrimaryContainerPink, onPrimaryContainer = OnPrimaryContainerPink,
    secondary = SecondaryPink, onSecondary = OnSecondaryPink, secondaryContainer = SecondaryContainerPink, onSecondaryContainer = OnSecondaryContainerPink,
    tertiary = TertiaryPink, onTertiary = OnTertiaryPink, tertiaryContainer = TertiaryContainerPink, onTertiaryContainer = OnTertiaryContainerPink,
    error = ErrorPink, onError = OnErrorPink, errorContainer = ErrorContainerPink, onErrorContainer = OnErrorContainerPink,
    background = Background, onBackground = OnBackground, surface = Surface, onSurface = OnSurface, surfaceVariant = SurfaceVariant, onSurfaceVariant = OnSurfaceVariant, outline = Outline
)

// 2. Gulf Blue (海湾蓝)
private val DarkGulfColorScheme = darkColorScheme(
    primary = PrimaryDarkGulf, onPrimary = OnPrimaryDarkGulf, primaryContainer = PrimaryContainerDarkGulf, onPrimaryContainer = OnPrimaryContainerDarkGulf,
    secondary = SecondaryDarkGulf, onSecondary = OnSecondaryDarkGulf, secondaryContainer = SecondaryContainerDarkGulf, onSecondaryContainer = OnSecondaryContainerDarkGulf,
    tertiary = TertiaryDarkGulf, onTertiary = OnTertiaryDarkGulf, tertiaryContainer = TertiaryContainerDarkGulf, onTertiaryContainer = OnTertiaryContainerDarkGulf,
    error = ErrorDarkGulf, onError = OnErrorDarkGulf, errorContainer = ErrorContainerDarkGulf, onErrorContainer = OnErrorContainerDarkGulf,
    background = BackgroundDark, onBackground = OnBackgroundDark, surface = SurfaceDark, onSurface = OnSurfaceDark, surfaceVariant = SurfaceVariantDark, onSurfaceVariant = OnSurfaceVariantDark, outline = OutlineDark
)
private val LightGulfColorScheme = lightColorScheme(
    primary = PrimaryGulf, onPrimary = OnPrimaryGulf, primaryContainer = PrimaryContainerGulf, onPrimaryContainer = OnPrimaryContainerGulf,
    secondary = SecondaryGulf, onSecondary = OnSecondaryGulf, secondaryContainer = SecondaryContainerGulf, onSecondaryContainer = OnSecondaryContainerGulf,
    tertiary = TertiaryGulf, onTertiary = OnTertiaryGulf, tertiaryContainer = TertiaryContainerGulf, onTertiaryContainer = OnTertiaryContainerGulf,
    error = ErrorGulf, onError = ErrorGulf, errorContainer = ErrorContainerGulf, onErrorContainer = OnErrorContainerGulf,
    background = Background, onBackground = OnBackground, surface = Surface, onSurface = OnSurface, surfaceVariant = SurfaceVariant, onSurfaceVariant = OnSurfaceVariant, outline = Outline
)

// 3. Field Green (原野绿)
private val DarkFieldColorScheme = darkColorScheme(
    primary = PrimaryDarkField, onPrimary = OnPrimaryDarkField, primaryContainer = PrimaryContainerDarkField, onPrimaryContainer = OnPrimaryContainerDarkField,
    secondary = SecondaryDarkField, onSecondary = OnSecondaryDarkField, secondaryContainer = SecondaryContainerDarkField, onSecondaryContainer = OnSecondaryContainerDarkField,
    tertiary = TertiaryDarkField, onTertiary = OnTertiaryDarkField, tertiaryContainer = TertiaryContainerDarkField, onTertiaryContainer = OnTertiaryContainerDarkField,
    error = ErrorDarkField, onError = OnErrorDarkField, errorContainer = ErrorContainerDarkField, onErrorContainer = OnErrorContainerDarkField,
    background = BackgroundDark, onBackground = OnBackgroundDark, surface = SurfaceDark, onSurface = OnSurfaceDark, surfaceVariant = SurfaceVariantDark, onSurfaceVariant = OnSurfaceVariantDark, outline = OutlineDark
)
private val LightFieldColorScheme = lightColorScheme(
    primary = PrimaryField, onPrimary = OnPrimaryField, primaryContainer = PrimaryContainerField, onPrimaryContainer = OnPrimaryContainerField,
    secondary = SecondaryField, onSecondary = OnSecondaryField, secondaryContainer = SecondaryContainerField, onSecondaryContainer = OnSecondaryContainerField,
    tertiary = TertiaryField, onTertiary = OnTertiaryField, tertiaryContainer = TertiaryContainerField, onTertiaryContainer = OnTertiaryContainerField,
    error = ErrorField, onError = OnErrorField, errorContainer = ErrorContainerField, onErrorContainer = OnErrorContainerField,
    background = Background, onBackground = OnBackground, surface = Surface, onSurface = OnSurface, surfaceVariant = SurfaceVariant, onSurfaceVariant = OnSurfaceVariant, outline = Outline
)

// 4. Autumn Yellow (秋黄)
private val DarkAutumnColorScheme = darkColorScheme(
    primary = PrimaryDarkAutumn, onPrimary = OnPrimaryDarkAutumn, primaryContainer = PrimaryContainerDarkAutumn, onPrimaryContainer = OnPrimaryContainerDarkAutumn,
    secondary = SecondaryDarkAutumn, onSecondary = OnSecondaryDarkAutumn, secondaryContainer = SecondaryContainerDarkAutumn, onSecondaryContainer = OnSecondaryContainerDarkAutumn,
    tertiary = TertiaryDarkAutumn, onTertiary = OnTertiaryDarkAutumn, tertiaryContainer = TertiaryContainerDarkAutumn, onTertiaryContainer = OnTertiaryContainerDarkAutumn,
    error = ErrorDarkAutumn, onError = OnErrorDarkAutumn, errorContainer = ErrorContainerDarkAutumn, onErrorContainer = OnErrorContainerDarkAutumn,
    background = BackgroundDark, onBackground = OnBackgroundDark, surface = SurfaceDark, onSurface = OnSurfaceDark, surfaceVariant = SurfaceVariantDark, onSurfaceVariant = OnSurfaceVariantDark, outline = OutlineDark
)
private val LightAutumnColorScheme = lightColorScheme(
    primary = PrimaryAutumn, onPrimary = OnPrimaryAutumn, primaryContainer = PrimaryContainerAutumn, onPrimaryContainer = OnPrimaryContainerAutumn,
    secondary = SecondaryAutumn, onSecondary = OnSecondaryAutumn, secondaryContainer = SecondaryContainerAutumn, onSecondaryContainer = OnSecondaryContainerAutumn,
    tertiary = TertiaryAutumn, onTertiary = OnTertiaryAutumn, tertiaryContainer = TertiaryContainerAutumn, onTertiaryContainer = OnTertiaryContainerAutumn,
    error = ErrorAutumn, onError = OnErrorAutumn, errorContainer = ErrorContainerAutumn, onErrorContainer = OnErrorContainerAutumn,
    background = Background, onBackground = OnBackground, surface = Surface, onSurface = OnSurface, surfaceVariant = SurfaceVariant, onSurfaceVariant = OnSurfaceVariant, outline = Outline
)

// 5. Neutral Black (中性黑)
private val DarkNeutralColorScheme = darkColorScheme(
    primary = PrimaryDarkNeutral, onPrimary = OnPrimaryDarkNeutral, primaryContainer = PrimaryContainerDarkNeutral, onPrimaryContainer = OnPrimaryContainerDarkNeutral,
    secondary = SecondaryDarkNeutral, onSecondary = OnSecondaryDarkNeutral, secondaryContainer = SecondaryContainerDarkNeutral, onSecondaryContainer = OnSecondaryContainerDarkNeutral,
    tertiary = TertiaryDarkNeutral, onTertiary = OnTertiaryDarkNeutral, tertiaryContainer = TertiaryContainerDarkNeutral, onTertiaryContainer = OnTertiaryContainerDarkNeutral,
    error = ErrorDarkNeutral, onError = OnErrorDarkNeutral, errorContainer = ErrorContainerDarkNeutral, onErrorContainer = OnErrorContainerDarkNeutral,
    background = BackgroundDark, onBackground = OnBackgroundDark, surface = SurfaceDark, onSurface = OnSurfaceDark, surfaceVariant = SurfaceVariantDark, onSurfaceVariant = OnSurfaceVariantDark, outline = OutlineDark
)
private val LightNeutralColorScheme = lightColorScheme(
    primary = PrimaryNeutral, onPrimary = OnPrimaryNeutral, primaryContainer = PrimaryContainerNeutral, onPrimaryContainer = OnPrimaryContainerNeutral,
    secondary = SecondaryNeutral, onSecondary = OnSecondaryNeutral, secondaryContainer = SecondaryContainerNeutral, onSecondaryContainer = OnSecondaryContainerNeutral,
    tertiary = TertiaryNeutral, onTertiary = OnTertiaryNeutral, tertiaryContainer = TertiaryContainerNeutral, onTertiaryContainer = OnTertiaryContainerNeutral,
    error = ErrorNeutral, onError = OnErrorNeutral, errorContainer = ErrorContainerNeutral, onErrorContainer = OnErrorContainerNeutral,
    background = Background, onBackground = OnBackground, surface = Surface, onSurface = OnSurface, surfaceVariant = SurfaceVariant, onSurfaceVariant = OnSurfaceVariant, outline = Outline
)

@Composable
fun ExpenseTrackerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = true,
    themeColor: String = "Pink",
    amoledDarkModeEnabled: Boolean = false,
    content: @Composable () -> Unit
) {
    val baseColorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        themeColor == "Pink" -> if (darkTheme) DarkPinkColorScheme else LightPinkColorScheme
        themeColor == "Gulf" -> if (darkTheme) DarkGulfColorScheme else LightGulfColorScheme
        themeColor == "Field" -> if (darkTheme) DarkFieldColorScheme else LightFieldColorScheme
        themeColor == "Autumn" -> if (darkTheme) DarkAutumnColorScheme else LightAutumnColorScheme
        themeColor == "Neutral" -> if (darkTheme) DarkNeutralColorScheme else LightNeutralColorScheme
        else -> if (darkTheme) DarkPinkColorScheme else LightPinkColorScheme
    }

    val colorScheme = if (darkTheme && amoledDarkModeEnabled) {
        baseColorScheme.copy(
            background = androidx.compose.ui.graphics.Color.Black,
            surface = androidx.compose.ui.graphics.Color.Black,
            surfaceVariant = androidx.compose.ui.graphics.Color(0xFF1E1E1E)
        )
    } else {
        baseColorScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
