package uz.xumoyun.core.ads

/**
 * Reklama bloklari ID lari. Ilova moduli BuildConfig orqali beradi:
 * debug'da doim [AdTestIds], release'da local.properties dagi haqiqiy ID lar.
 */
data class AdUnitIds(
    val banner: String = AdTestIds.BANNER,
    val interstitial: String = AdTestIds.INTERSTITIAL,
    val rewarded: String = AdTestIds.REWARDED,
    val appOpen: String = AdTestIds.APP_OPEN,
)

/** Ilova qaysi reklama turlarini ishlatadi (masalan, Namoz ilovasida interstitial yo'q). */
data class AdFeatures(
    val banner: Boolean = true,
    val interstitial: Boolean = true,
    val rewarded: Boolean = true,
    val appOpen: Boolean = true,
)

/** Google'ning rasmiy test ID lari. Ishlab chiqishda faqat shular ishlatiladi. */
object AdTestIds {
    const val APP_ID = "ca-app-pub-3940256099942544~3347511713"
    const val BANNER = "ca-app-pub-3940256099942544/9214589741"
    const val INTERSTITIAL = "ca-app-pub-3940256099942544/1033173712"
    const val REWARDED = "ca-app-pub-3940256099942544/5224354917"
    const val APP_OPEN = "ca-app-pub-3940256099942544/9257395921"
}
