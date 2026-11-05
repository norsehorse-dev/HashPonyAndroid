package com.hashpony.android.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.hashpony.android.R

// Running UI text uses the platform sans (Material's default typography).
// IBM Plex Mono is reserved for the things that really are monospace data:
// hex digests, sums-file content and the hash input field.
val PlexMono = FontFamily(
    Font(R.font.ibm_plex_mono_regular, FontWeight.Normal),
    Font(R.font.ibm_plex_mono_medium, FontWeight.Medium),
    Font(R.font.ibm_plex_mono_semibold, FontWeight.SemiBold),
)

val HashPonyTypography = Typography()

/** Hex digest readout: columns of digits line up and scan cleanly. */
val MonoDigestStyle = TextStyle(
    fontFamily = PlexMono,
    fontWeight = FontWeight.Medium,
    fontSize = 13.sp,
    lineHeight = 19.sp,
)

/** Default UI text for names, buttons, list rows (same as bodyLarge). */
val UiStyle = TextStyle(
    fontWeight = FontWeight.Normal,
    fontSize = 16.sp,
    lineHeight = 24.sp,
)

/** Secondary line: sizes, statuses, algorithm names, captions. */
val MetaStyle = TextStyle(
    fontWeight = FontWeight.Normal,
    fontSize = 14.sp,
    lineHeight = 20.sp,
)
