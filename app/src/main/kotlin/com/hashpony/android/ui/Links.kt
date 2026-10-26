package com.hashpony.android.ui

import androidx.annotation.StringRes
import com.hashpony.android.R

/**
 * External links. Opening a link hands off to the browser, so it needs no
 * INTERNET permission of our own.
 */
object Links {
    /** The Pony family hub (norsehor.se itself has no public site). */
    const val HUB = "https://pony.norsehor.se"
    const val SOURCE = "https://github.com/norsehorse-dev/HashPonyAndroid"

    /** A sibling Pony app: brand name (never translated), product site, tagline. */
    data class PonyApp(val name: String, val url: String, @StringRes val tagline: Int)

    /** Every other Pony app, each linked to its own product site, in the order hashpony.app lists them. */
    val FAMILY: List<PonyApp> = listOf(
        PonyApp("PGPony", "https://pgpony.app", R.string.family_pgpony),
        PonyApp("PassPony", "https://passpony.app", R.string.family_passpony),
        PonyApp("VaultPony", "https://vaultpony.app", R.string.family_vaultpony),
        PonyApp("AgePony", "https://agepony.com", R.string.family_agepony),
        PonyApp("CarrierPony", "https://carrierpony.com", R.string.family_carrierpony),
        PonyApp("BurnPony", "https://burnpony.app", R.string.family_burnpony),
        PonyApp("RelayPony", "https://relaypony.app", R.string.family_relaypony),
        PonyApp("QuorumPony", "https://quorumpony.com", R.string.family_quorumpony),
        PonyApp("ScrubPony", "https://scrubpony.app", R.string.family_scrubpony),
        PonyApp("TapPony", "https://tappony.app", R.string.family_tappony),
    )
}
