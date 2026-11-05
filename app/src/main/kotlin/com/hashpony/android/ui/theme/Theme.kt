package com.hashpony.android.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

// The Instrument: a bespoke silver-on-charcoal identity. Dynamic (Material You)
// color is intentionally not used — the palette is the brand.
private val InstrumentDark = darkColorScheme(
    primary = Silver,
    onPrimary = Ground,
    secondary = SilverDim,
    onSecondary = Ground,
    background = Ground,
    onBackground = TextPrimary,
    surface = Panel,
    onSurface = TextPrimary,
    surfaceVariant = PanelElev,
    onSurfaceVariant = TextDim,
    primaryContainer = PanelHigh,
    onPrimaryContainer = SilverBright,
    outline = Line,
    outlineVariant = LineSoft,
    error = FailRed,
    onError = Ground,
    // Pin every surface role M3 components reach for (menus, dialogs, the nav
    // bar indicator, snackbars) so nothing falls back to the baseline purples.
    surfaceTint = Silver,
    surfaceDim = Ground,
    surfaceBright = PanelHigh,
    surfaceContainerLowest = Ground,
    surfaceContainerLow = Panel,
    surfaceContainer = PanelElev,
    surfaceContainerHigh = PanelElev,
    surfaceContainerHighest = PanelHigh,
    secondaryContainer = PanelHigh,
    onSecondaryContainer = SilverBright,
    tertiary = MatchGreen,
    onTertiary = Ground,
    inverseSurface = SilverBright,
    inverseOnSurface = Ground,
    scrim = Ground,
)

/**
 * HashPony theme. Signature is preserved for MainActivity; for now it commits to
 * the dark Instrument scheme regardless of the passed flags, since that is the
 * 1.0 identity. Light and a user toggle come back once the dark look is locked.
 */
@Composable
fun HashPonyTheme(
    darkTheme: Boolean = true,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = InstrumentDark,
        typography = HashPonyTypography,
        content = content,
    )
}
