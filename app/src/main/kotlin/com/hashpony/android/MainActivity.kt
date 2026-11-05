package com.hashpony.android

import android.content.Intent
import android.content.res.Configuration
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.hashpony.android.data.PreferencesRepository
import com.hashpony.android.data.ThemeMode
import com.hashpony.android.i18n.LanguageState
import com.hashpony.android.ui.HashPonyApp
import com.hashpony.android.ui.theme.HashPonyTheme

// AppCompatActivity rather than ComponentActivity so the in-app language
// picker also works on Android 8 to 12 (see i18n/LanguageManager.kt).
class MainActivity : AppCompatActivity() {

    // Incoming work, held at activity level so a second intent (onNewIntent,
    // e.g. the Quick Settings tile while the app is open) reaches the UI too.
    private var sharedUris by mutableStateOf<List<Uri>>(emptyList())
    private var verifyUri by mutableStateOf<Uri?>(null)
    private var pickFilesRequest by mutableIntStateOf(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        LanguageState.init(this)
        // The Instrument is dark-only for now, so system bar icons stay light even
        // when the device is in light mode.
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
        )

        // Only a fresh launch consumes the intent; a rotation must not re-open
        // the picker or restart a verify that already has results.
        if (savedInstanceState == null) handleIntent(intent)

        setContent {
            val prefs = remember { PreferencesRepository(applicationContext) }
            val settings by prefs.settings.collectAsState(initial = null)

            val dark = when (settings?.themeMode) {
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
                else -> isSystemInDarkTheme()
            }
            val dynamic = settings?.dynamicColor ?: true

            HashPonyTheme(darkTheme = dark, dynamicColor = dynamic) {
                Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    HashPonyApp(
                        sharedUris = sharedUris,
                        onSharedConsumed = { sharedUris = emptyList() },
                        verifyUri = verifyUri,
                        onVerifyConsumed = { verifyUri = null },
                        pickFilesRequest = pickFilesRequest,
                        onPickFilesConsumed = { pickFilesRequest = 0 },
                    )
                }
            }
        }
    }

    // A language switch lands here in place (configChanges in the manifest).
    // Re-read it so a change made in system Settings also shows in the picker.
    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        LanguageState.init(this)
        // On Android 8 to 12, AppCompat 1.7 applies the new language in place but
        // does not pass the change to the view tree, so Compose would keep the old
        // strings. Pass it on. On 13+ the framework already did; a repeat is harmless.
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            window.decorView.dispatchConfigurationChanged(newConfig)
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        when (intent?.action) {
            ACTION_PICK_FILES -> pickFilesRequest++
            Intent.ACTION_SEND, Intent.ACTION_SEND_MULTIPLE -> sharedUris = extractSharedUris(intent)
            Intent.ACTION_VIEW -> verifyUri = extractVerifyUri(intent)
        }
    }

    /** Files shared in for hashing (SEND / SEND_MULTIPLE). */
    private fun extractSharedUris(intent: Intent?): List<Uri> {
        intent ?: return emptyList()
        return when (intent.action) {
            Intent.ACTION_SEND -> listOfNotNull(intent.parcelableExtra(Intent.EXTRA_STREAM))
            Intent.ACTION_SEND_MULTIPLE -> intent.parcelableArrayListExtra(Intent.EXTRA_STREAM) ?: emptyList()
            else -> emptyList()
        }
    }

    /** A sums file opened with HashPony (VIEW) goes to the Verify flow. */
    private fun extractVerifyUri(intent: Intent?): Uri? =
        if (intent?.action == Intent.ACTION_VIEW) intent.data else null

    @Suppress("DEPRECATION")
    private inline fun <reified T : android.os.Parcelable> Intent.parcelableExtra(key: String): T? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) getParcelableExtra(key, T::class.java)
        else getParcelableExtra(key) as? T

    @Suppress("DEPRECATION")
    private inline fun <reified T : android.os.Parcelable> Intent.parcelableArrayListExtra(key: String): ArrayList<T>? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) getParcelableArrayListExtra(key, T::class.java)
        else getParcelableArrayListExtra(key)

    companion object {
        /** Sent by the Quick Settings tile: open Hash / Files and launch the picker. */
        const val ACTION_PICK_FILES = "com.hashpony.android.action.PICK_FILES"
    }
}
