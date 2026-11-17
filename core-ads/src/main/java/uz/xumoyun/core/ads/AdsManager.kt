package uz.xumoyun.core.ads

import android.app.Activity
import android.content.Context
import android.util.Log
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.appopen.AppOpenAd
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import com.google.android.ump.ConsentInformation
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.Instant
import java.time.LocalDate
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "Ads"

/**
 * Barcha reklama turlari bir joyda. Asosiy qoida: reklama yuklanmasa yoki
 * rozilik berilmasa, ilova hech narsani kutmaydi — callback darhol chaqiriladi.
 */
@Singleton
class AdsManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val ids: AdUnitIds,
    private val features: AdFeatures,
) {
    private val consentInfo: ConsentInformation = UserMessagingPlatform.getConsentInformation(context)
    private val initialized = AtomicBoolean(false)
    private val _ready = MutableStateFlow(false)

    /** Reklama so'rash mumkin (rozilik olingan va SDK ishga tushgan). */
    val ready: StateFlow<Boolean> = _ready.asStateFlow()

    val bannerEnabled get() = features.banner
    val bannerUnitId get() = ids.banner

    private val interstitialPolicy = InterstitialPolicy()
    private var interstitial: InterstitialAd? = null
    private var interstitialLoading = false

    private var rewarded: RewardedAd? = null
    private var rewardedLoading = false
    private val _rewardedReady = MutableStateFlow(false)
    val rewardedReady: StateFlow<Boolean> = _rewardedReady.asStateFlow()

    private var appOpen: AppOpenAd? = null
    private val prefs = context.getSharedPreferences("ads", Context.MODE_PRIVATE)

    /** UMP rozilik oynasi (kerak bo'lsa), keyin SDK ni ishga tushirish. Activity.onCreate da chaqiriladi. */
    fun gatherConsent(activity: Activity) {
        val params = ConsentRequestParameters.Builder().build()
        consentInfo.requestConsentInfoUpdate(activity, params, {
            UserMessagingPlatform.loadAndShowConsentFormIfRequired(activity) { error ->
                if (error != null) Log.w(TAG, "Consent form: ${error.message}")
                initIfAllowed()
            }
        }, { error ->
            Log.w(TAG, "Consent update: ${error.message}")
            initIfAllowed()
        })
        // Oldingi sessiyada rozilik olingan bo'lsa, kutmasdan boshlaymiz.
        initIfAllowed()
    }

    val privacyOptionsRequired: Boolean
        get() = consentInfo.privacyOptionsRequirementStatus ==
            ConsentInformation.PrivacyOptionsRequirementStatus.REQUIRED

    fun showPrivacyOptions(activity: Activity) {
        UserMessagingPlatform.showPrivacyOptionsForm(activity) { initIfAllowed() }
    }

    private fun initIfAllowed() {
        if (!consentInfo.canRequestAds()) return
        if (!initialized.compareAndSet(false, true)) return
        MobileAds.initialize(context) {
            _ready.value = true
            preloadInterstitial()
            preloadRewarded()
        }
    }

    // ---------------- Interstitial ----------------

    fun preloadInterstitial() {
        if (!features.interstitial || !_ready.value || interstitial != null || interstitialLoading) return
        interstitialLoading = true
        InterstitialAd.load(context, ids.interstitial, AdRequest.Builder().build(), object : InterstitialAdLoadCallback() {
            override fun onAdLoaded(ad: InterstitialAd) { interstitial = ad; interstitialLoading = false }
            override fun onAdFailedToLoad(error: LoadAdError) { interstitialLoading = false; Log.d(TAG, "Interstitial: ${error.message}") }
        })
    }

    /** Tabiiy tanaffusda chaqiriladi. Ko'rsatib bo'lmasa [onDone] darhol ishlaydi. */
    fun showInterstitial(activity: Activity, onDone: () -> Unit) {
        val ad = interstitial
        val now = System.currentTimeMillis()
        if (!features.interstitial || ad == null || !interstitialPolicy.canShow(now)) {
            preloadInterstitial()
            onDone()
            return
        }
        interstitial = null
        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() { preloadInterstitial(); onDone() }
            override fun onAdFailedToShowFullScreenContent(error: AdError) { preloadInterstitial(); onDone() }
        }
        interstitialPolicy.recordShown(now)
        ad.show(activity)
    }

    // ---------------- Rewarded ----------------

    fun preloadRewarded() {
        if (!features.rewarded || !_ready.value || rewarded != null || rewardedLoading) return
        rewardedLoading = true
        RewardedAd.load(context, ids.rewarded, AdRequest.Builder().build(), object : RewardedAdLoadCallback() {
            override fun onAdLoaded(ad: RewardedAd) { rewarded = ad; rewardedLoading = false; _rewardedReady.value = true }
            override fun onAdFailedToLoad(error: LoadAdError) { rewardedLoading = false; Log.d(TAG, "Rewarded: ${error.message}") }
        })
    }

    /**
     * Mukofotli reklama. Reklama yo'q bo'lsa foydalanuvchini kuttirmaslik uchun
     * mukofot darhol beriladi ([onReward] chaqiriladi).
     */
    fun showRewarded(activity: Activity, onReward: () -> Unit) {
        val ad = rewarded
        if (ad == null) {
            preloadRewarded()
            onReward()
            return
        }
        rewarded = null
        _rewardedReady.value = false
        var earned = false
        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() { preloadRewarded(); if (earned) onReward() }
            override fun onAdFailedToShowFullScreenContent(error: AdError) { preloadRewarded(); onReward() }
        }
        ad.show(activity) { earned = true }
    }

    // ---------------- App Open ----------------

    /** Ilova ochilganda, kuniga ko'pi bilan 1 marta. Yuklanmasa — hech narsa qilinmaydi. */
    fun showAppOpenIfEligible(activity: Activity) {
        if (!features.appOpen || !_ready.value) return
        val last = prefs.getString(KEY_APP_OPEN_DAY, null)?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
        if (!AppOpenPolicy.canShow(last, Instant.now())) return
        val cached = appOpen
        if (cached != null) {
            showAppOpen(activity, cached)
            return
        }
        AppOpenAd.load(context, ids.appOpen, AdRequest.Builder().build(), object : AppOpenAd.AppOpenAdLoadCallback() {
            override fun onAdLoaded(ad: AppOpenAd) {
                if (!activity.isFinishing) showAppOpen(activity, ad) else appOpen = ad
            }
            override fun onAdFailedToLoad(error: LoadAdError) { Log.d(TAG, "AppOpen: ${error.message}") }
        })
    }

    private fun showAppOpen(activity: Activity, ad: AppOpenAd) {
        appOpen = null
        prefs.edit().putString(KEY_APP_OPEN_DAY, LocalDate.now().toString()).apply()
        ad.show(activity)
    }

    private companion object {
        const val KEY_APP_OPEN_DAY = "app_open_day"
    }
}
