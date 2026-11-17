package uz.xumoyun.namoz

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dagger.hilt.android.AndroidEntryPoint
import uz.xumoyun.core.ads.AdsManager
import uz.xumoyun.core.common.ThemeMode
import uz.xumoyun.core.data.CommonSettings
import uz.xumoyun.core.ui.theme.AppTheme
import uz.xumoyun.namoz.ui.LocalAds
import uz.xumoyun.namoz.ui.NamozPalette
import uz.xumoyun.namoz.ui.NamozRoot
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    @Inject lateinit var ads: AdsManager
    @Inject lateinit var common: CommonSettings

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        ads.gatherConsent(this)
        setContent {
            val theme by common.themeMode.collectAsStateWithLifecycle(ThemeMode.SYSTEM)
            val onboardingDone by common.onboardingDone.collectAsStateWithLifecycle(null)
            AppTheme(NamozPalette, theme) {
                CompositionLocalProvider(LocalAds provides ads) {
                    onboardingDone?.let { NamozRoot(startWithOnboarding = !it) }
                }
            }
        }
    }
}
