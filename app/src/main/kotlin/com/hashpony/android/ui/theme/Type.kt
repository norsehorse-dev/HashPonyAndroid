package com.hashpony.android.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.hashpony.android.R

// IBM Plex Mono carries the instrument identity: every hash, every uppercase
// label. UI running text stays on the platform sans.
val PlexMono = FontFamily(
    Font(R.font.ibm_plex_mono_regular, FontWeight.Normal),
    Font(R.font.ibm_plex_mono_medium, FontWeight.Medium),
    Font(R.font.ibm_plex_mono_semibold, FontWeight.SemiBold),
)

val HashPonyTypography = Typography()

// Monospace hex readout — columns of digits line up and scan cleanly.
val MonoDigestStyle = TextStyle(
    fontFamily = PlexMono,
    fontWeight = FontWeight.Medium,
    fontSize = 13.sp,
    lineHeight = 19.sp,
)

// Small uppercase section label, e.g. "// ALGORITHM KIT".
val MonoLabelStyle = TextStyle(
    fontFamily = PlexMono,
    fontWeight = FontWeight.Medium,
    fontSize = 11.sp,
    letterSpacing = 1.sp,
)

// The [hashpony] wordmark and chip text.
val MonoUiStyle = TextStyle(
    fontFamily = PlexMono,
    fontWeight = FontWeight.Medium,
    fontSize = 13.sp,
)
