package com.hashpony.android.data

import com.hashpony.core.AlgorithmRegistry

/**
 * Preset algorithm selections. Picking a preset replaces the enabled set; any
 * manual toggle (Settings) is remembered as the Custom kit so the user can get
 * back to it. The active kit is derived from the enabled set rather than stored,
 * so toggling your way to exactly the Forensics set lights up Forensics.
 */
enum class AlgorithmKit {
    DOWNLOADS,
    FORENSICS,
    EVERYTHING,
    CUSTOM;

    /** The algorithm ids this preset enables, or null for [CUSTOM]. */
    val presetIds: Set<String>?
        get() = when (this) {
            DOWNLOADS -> setOf("sha256", "sha512", "sha1", "md5")
            FORENSICS -> setOf("md5", "sha1", "sha256")
            EVERYTHING -> AlgorithmRegistry.all.map { it.id }.toSet()
            CUSTOM -> null
        }

    companion object {
        /** The preset whose set equals [enabled] exactly, else [CUSTOM]. */
        fun matching(enabled: Set<String>): AlgorithmKit =
            entries.firstOrNull { it.presetIds == enabled } ?: CUSTOM
    }
}
