package uz.xumoyun.feature.ramadan.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import uz.xumoyun.feature.ramadan.R
import uz.xumoyun.feature.ramadan.share.RamadanShare
import uz.xumoyun.feature.ramadan.share.ShareCardText
import java.util.Locale

/**
 * Ramazon menyusi grafi (TZ 6-bo'lim): R1 bosh sahifa, R2 taqvim, R3 ro'za, R4 duolar, R5 xatm, R6 sozlamalar.
 * [onOpenMushaf] — xatmdan mus'hafga o'tish (navigatsiyani ilova beradi).
 */
fun NavGraphBuilder.ramadanGraph(nav: NavHostController, onOpenMushaf: () -> Unit) {
    navigation(startDestination = RamadanRoutes.HOME, route = RamadanRoutes.GRAPH) {
        composable(RamadanRoutes.HOME) {
            RamadanHomeRoute(
                onOpenDays = { nav.navigate(RamadanRoutes.DAYS) },
                onOpenDuas = { nav.navigate(RamadanRoutes.DUAS) },
                onOpenKhatm = { nav.navigate(RamadanRoutes.KHATM) },
                onOpenFasts = { nav.navigate(RamadanRoutes.FASTS) },
                onOpenSettings = { nav.navigate(RamadanRoutes.SETTINGS) },
            )
        }
        composable(RamadanRoutes.DAYS) {
            val context = LocalContext.current
            RamadanDaysRoute(onShare = { s ->
                val locale = Locale.getDefault()
                val zone = java.time.ZoneId.systemDefault()
                val times: (java.time.Instant?) -> String = { timeText(it, zone, locale) }
                RamadanShare.share(
                    context,
                    ShareCardText(
                        title = "${s.city.orEmpty()} · " + context.getString(R.string.hijri_year_label, s.hijriYear),
                        columns = RamadanShare.rowsText(s.rows, times, locale),
                        appName = context.getString(R.string.share_app_name),
                        playLink = RamadanShare.PLAY_LINK,
                    ),
                    fileName = "ramadan-${s.hijriYear}.png",
                )
            })
        }
        composable(RamadanRoutes.FASTS) { FastTrackerRoute() }
        composable(RamadanRoutes.DUAS) { DuasRoute() }
        composable(RamadanRoutes.KHATM) { KhatmRoute(onOpenMushaf = onOpenMushaf) }
        composable(RamadanRoutes.SETTINGS) { RamadanSettingsRoute() }
    }
}

/** Ramazon sarlavhasi (app'dagi yuqori panel uchun). */
@Composable
fun ramadanTitle(route: String?): String? = when (route) {
    RamadanRoutes.HOME -> stringResource(R.string.ramadan_title)
    RamadanRoutes.DAYS -> stringResource(R.string.ramadan_calendar_title)
    RamadanRoutes.FASTS -> stringResource(R.string.ramadan_fasts_title)
    RamadanRoutes.DUAS -> stringResource(R.string.ramadan_duas_title)
    RamadanRoutes.KHATM -> stringResource(R.string.ramadan_khatm_title)
    RamadanRoutes.SETTINGS -> stringResource(R.string.ramadan_settings_title)
    else -> null
}
