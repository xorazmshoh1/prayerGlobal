package uz.xumoyun.namoz

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dagger.hilt.android.AndroidEntryPoint
import uz.xumoyun.core.common.ThemeMode
import uz.xumoyun.core.data.CommonSettings
import uz.xumoyun.core.ui.theme.AppTheme
import uz.xumoyun.namoz.ui.NamozPalette
import uz.xumoyun.namoz.ui.NamozRoot
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    @Inject lateinit var common: CommonSettings

    /** Ramazon yorlig'i (ShortcutManager) yoki vidjetdan ochilganda true. */
    private var openRamadan by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        openRamadan = isRamadanIntent(intent)
        setContent {
            val theme by common.themeMode.collectAsStateWithLifecycle(ThemeMode.SYSTEM)
            val onboardingDone by common.onboardingDone.collectAsStateWithLifecycle(null)
            AppTheme(NamozPalette, theme) {
                onboardingDone?.let { NamozRoot(startWithOnboarding = !it, openRamadan = openRamadan) }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        openRamadan = isRamadanIntent(intent)
    }

    private fun isRamadanIntent(intent: Intent?): Boolean = intent?.action == ACTION_RAMADAN

    companion object {
        const val ACTION_RAMADAN = "uz.xumoyun.namoz.action.RAMADAN"
    }
}
