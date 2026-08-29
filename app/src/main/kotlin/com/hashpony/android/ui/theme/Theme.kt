package com.hashpony.android.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val LightColors = lightColorScheme(
    primary = SteelSilverDark,
    onPrimary = White,
    secondary = SteelSilver,
    background = BackgroundLight,
    surface = White,
)

private val DarkColors = darkColorScheme(
    primary = SteelSilver,
    onPrimary = Graphite,
    secondary = SteelSilverDark,
    background = Graphite,
    surface = GraphiteElev,
)

/**
 * HashPony theme. Uses Material You dynamic color on Android 12+, falling back to
 * the steel-silver brand scheme. [dynamicColor] can be turned off from Settings.
 */
@Composable
fun HashPonyTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColors
        else -> LightColors
    }
    MaterialTheme(
        colorScheme = colorScheme,
        typography = HashPonyTypography,
        content = content,
    )
}
