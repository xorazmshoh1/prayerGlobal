package uz.xumoyun.namoz.ui

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.AutoStories
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Explore
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.TouchApp
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.navigation
import androidx.navigation.compose.rememberNavController
import uz.xumoyun.feature.hifz.HifzRoute
import uz.xumoyun.feature.hifz.HifzRoutes
import uz.xumoyun.feature.hifz.hifzGraph
import uz.xumoyun.feature.hifz.audio.AudioRoutes
import uz.xumoyun.feature.hifz.audio.audioGraph
import uz.xumoyun.feature.hifz.MushafRoutes
import uz.xumoyun.feature.hifz.mushafGraph
import uz.xumoyun.feature.prayer.ui.LocationsScreen
import uz.xumoyun.feature.prayer.ui.NotificationSettingsRoute
import uz.xumoyun.feature.prayer.ui.PrayerRoute
import uz.xumoyun.feature.ramadan.RamadanHomeRoute
import uz.xumoyun.feature.ramadan.RamadanRoutes
import uz.xumoyun.namoz.R
import uz.xumoyun.namoz.ui.calendar.CalendarRoute
import uz.xumoyun.namoz.ui.onboarding.OnboardingRoute
import uz.xumoyun.feature.qibla.ui.QiblaRoute
import uz.xumoyun.namoz.ui.settings.HelpScreen
import uz.xumoyun.namoz.ui.settings.LicensesScreen
import uz.xumoyun.feature.prayer.tracker.ui.TrackerRoute
import uz.xumoyun.namoz.ui.settings.SettingsRoute
import uz.xumoyun.namoz.ui.settings.SettingsViewModel
import uz.xumoyun.feature.tasbih.ui.TasbihRoute

/** TZ 6-bo'lim: pastki navigatsiyada 5 ta tab. Reklama hech bir ekranda yo'q. */
private enum class Tab(val route: String, @StringRes val label: Int, val icon: ImageVector) {
    PRAYER("prayer", R.string.tab_prayer, Icons.Rounded.Schedule),
    CALENDAR("calendar", R.string.tab_calendar, Icons.Rounded.CalendarMonth),
    HIFZ("hifz", uz.xumoyun.feature.hifz.R.string.hifz_title, Icons.Rounded.AutoStories),
    QIBLA("qibla", R.string.tab_qibla, Icons.Rounded.Explore),
    TASBEH("tasbeh", R.string.tab_tasbeh, Icons.Rounded.TouchApp),
}

private object Routes {
    const val ONBOARDING = "onboarding"
    /** Sozlamalar o'z sarlavhasini chizadi (yuqori panel yo'q). */
    const val SETTINGS = "settings"
    const val LOCATIONS = "locations"
    const val NOTIFICATIONS = "notifications"
    const val HELP = "help"
    /** TZ 9: namoz kuzatuvchisi va qazo (Namoz tabidan). */
    const val TRACKER = "tracker"
    /** TZ 12: Litsenziyalar va manbalar (Sozlamalardan). */
    const val LICENSES = "licenses"
}

@Composable
fun NamozRoot(startWithOnboarding: Boolean) {
    val nav = rememberNavController()
    val entry by nav.currentBackStackEntryAsState()
    val route = entry?.destination?.route
    val tab = Tab.entries.firstOrNull { it.route == route }
    val ownHeader = tab != null || route == Routes.ONBOARDING || route == Routes.SETTINGS

    Scaffold(
        topBar = { SubScreenBar(route, nav) },
        bottomBar = {
            if (tab != null) {
                NavigationBar {
                    Tab.entries.forEach { t ->
                        NavigationBarItem(
                            selected = t == tab,
                            onClick = {
                                nav.navigate(t.route) {
                                    popUpTo(nav.graph.findStartDestination().id) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(t.icon, null) },
                            label = { Text(stringResource(t.label)) },
                        )
                    }
                }
            }
        },
    ) { padding ->
        NavHost(
            navController = nav,
            startDestination = if (startWithOnboarding) Routes.ONBOARDING else Tab.PRAYER.route,
            modifier = Modifier.fillMaxSize().padding(bottom = padding.calculateBottomPadding(), top = if (!ownHeader) padding.calculateTopPadding() else androidx.compose.ui.unit.Dp(0f)),
        ) {
            composable(Routes.ONBOARDING) {
                val vm: SettingsViewModel = hiltViewModel()
                OnboardingRoute(vm) {
                    nav.navigate(Tab.PRAYER.route) { popUpTo(Routes.ONBOARDING) { inclusive = true } }
                }
            }
            composable(Tab.PRAYER.route) {
                PrayerRoute(
                    onPickLocation = { nav.navigate(Routes.LOCATIONS) },
                    onRamadan = { nav.navigate(RamadanRoutes.GRAPH) },
                    onSettings = { nav.navigate(Routes.SETTINGS) },
                    onTracker = { nav.navigate(Routes.TRACKER) },
                )
            }
            composable(Tab.HIFZ.route) {
                HifzRoute(
                    onStartLesson = { type -> nav.navigate(HifzRoutes.lesson(type)) },
                    onOpenPlan = { nav.navigate(HifzRoutes.PLAN) },
                    onOpenProgress = { nav.navigate(HifzRoutes.PROGRESS) },
                    onOpenMushaf = { nav.navigate(MushafRoutes.HOME) },
                    onOpenAudio = { nav.navigate(AudioRoutes.HOME) },
                )
            }
            // Hifz: dars rejimi, reja va progress (TZ 6-bo'lim, 5–7-ekranlar). Hifz tabidan ochiladi.
            hifzGraph(nav)
            // Mus'haf (TZ 5-bosqich): sura/juz ro'yxati, qidiruv, o'qish. Hifz tabidan ochiladi.
            mushafGraph(nav)
            // Audio boshqaruvi (TZ 6-bosqich): qori, sura bo'yicha yuklash/o'chirish. Hifz tabidan ochiladi.
            audioGraph(nav)
            composable(Tab.QIBLA.route) { QiblaRoute() }
            composable(Tab.TASBEH.route) { TasbihRoute() }
            composable(Tab.CALENDAR.route) { CalendarRoute(onRamadan = { nav.navigate(RamadanRoutes.GRAPH) }) }
            // Alohida Ramazon menyusi grafi: R1 bosh sahifa; R2–R6 11-bosqichda qo'shiladi.
            navigation(startDestination = RamadanRoutes.HOME, route = RamadanRoutes.GRAPH) {
                composable(RamadanRoutes.HOME) { RamadanHomeRoute() }
            }
            composable(Routes.SETTINGS) {
                SettingsRoute(
                    hiltViewModel(),
                    onLocations = { nav.navigate(Routes.LOCATIONS) },
                    onNotifications = { nav.navigate(Routes.NOTIFICATIONS) },
                    onHelp = { nav.navigate(Routes.HELP) },
                    onLicenses = { nav.navigate(Routes.LICENSES) },
                )
            }
            composable(Routes.LOCATIONS) { LocationsScreen(onPicked = { nav.popBackStack() }) }
            composable(Routes.NOTIFICATIONS) { NotificationSettingsRoute() }
            composable(Routes.HELP) { HelpScreen() }
            composable(Routes.TRACKER) { TrackerRoute() }
            composable(Routes.LICENSES) { LicensesScreen() }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SubScreenBar(route: String?, nav: NavHostController) {
    val title = when (route) {
        RamadanRoutes.HOME -> uz.xumoyun.feature.ramadan.R.string.ramadan_title
        Routes.LOCATIONS -> uz.xumoyun.feature.prayer.R.string.saved_locations
        Routes.NOTIFICATIONS -> R.string.notifications
        Routes.HELP -> R.string.help_notifications
        Routes.TRACKER -> uz.xumoyun.feature.prayer.R.string.tracker_title
        Routes.LICENSES -> R.string.licenses_title
        else -> return
    }
    TopAppBar(
        title = { Text(stringResource(title)) },
        navigationIcon = {
            IconButton(onClick = { nav.popBackStack() }) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, stringResource(R.string.back)) }
        },
    )
}
