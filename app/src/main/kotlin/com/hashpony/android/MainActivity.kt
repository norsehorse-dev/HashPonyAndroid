package com.hashpony.android

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.hashpony.android.data.PreferencesRepository
import com.hashpony.android.data.ThemeMode
import com.hashpony.android.ui.HashPonyApp
import com.hashpony.android.ui.theme.HashPonyTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val initialShared = extractSharedUris(intent)
        val initialVerify = extractVerifyUri(intent)

        setContent {
            val prefs = remember { PreferencesRepository(applicationContext) }
            val settings by prefs.settings.collectAsState(initial = null)

            val dark = when (settings?.themeMode) {
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
                else -> isSystemInDarkTheme()
            }
            val dynamic = settings?.dynamicColor ?: true

            var shared by remember { mutableStateOf(initialShared) }
            var verify by remember { mutableStateOf(initialVerify) }

            HashPonyTheme(darkTheme = dark, dynamicColor = dynamic) {
                Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    HashPonyApp(
                        sharedUris = shared,
                        onSharedConsumed = { shared = emptyList() },
                        verifyUri = verify,
                        onVerifyConsumed = { verify = null },
                    )
                }
            }
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
}
