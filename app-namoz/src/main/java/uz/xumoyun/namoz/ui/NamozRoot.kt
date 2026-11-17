package uz.xumoyun.namoz.ui

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Explore
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Settings
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
import androidx.navigation.compose.rememberNavController
import uz.xumoyun.core.ads.BannerAd
import uz.xumoyun.namoz.R
import uz.xumoyun.namoz.ui.calendar.CalendarRoute
import uz.xumoyun.namoz.ui.home.HomeRoute
import uz.xumoyun.namoz.ui.onboarding.OnboardingRoute
import uz.xumoyun.namoz.ui.qibla.QiblaRoute
import uz.xumoyun.namoz.ui.settings.HelpScreen
import uz.xumoyun.namoz.ui.settings.LocationsScreen
import uz.xumoyun.namoz.ui.settings.NotificationSettingsRoute
import uz.xumoyun.namoz.ui.settings.SettingsRoute
import uz.xumoyun.namoz.ui.settings.SettingsViewModel
import uz.xumoyun.namoz.ui.tasbeh.TasbehRoute

private enum class Tab(val route: String, @StringRes val label: Int, val icon: ImageVector, val banner: Boolean) {
    HOME("home", R.string.tab_home, Icons.Rounded.Home, banner = true),
    CALENDAR("calendar", R.string.tab_calendar, Icons.Rounded.CalendarMonth, banner = true),
    // TZ: qibla va tasbeh ekranlarida reklama yo'q.
    QIBLA("qibla", R.string.tab_qibla, Icons.Rounded.Explore, banner = false),
    TASBEH("tasbeh", R.string.tab_tasbeh, Icons.Rounded.TouchApp, banner = false),
    SETTINGS("settings", R.string.tab_settings, Icons.Rounded.Settings, banner = false),
}

private object Routes {
    const val ONBOARDING = "onboarding"
    const val LOCATIONS = "locations"
    const val NOTIFICATIONS = "notifications"
    const val HELP = "help"
}

@Composable
fun NamozRoot(startWithOnboarding: Boolean) {
    val nav = rememberNavController()
    val entry by nav.currentBackStackEntryAsState()
    val route = entry?.destination?.route
    val tab = Tab.entries.firstOrNull { it.route == route }
    val ads = LocalAds.current

    Scaffold(
        topBar = { SubScreenBar(route, nav) },
        bottomBar = {
            if (tab != null) {
                Column {
                    if (tab.banner && ads != null) BannerAd(ads)
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
            }
        },
    ) { padding ->
        NavHost(
            navController = nav,
            startDestination = if (startWithOnboarding) Routes.ONBOARDING else Tab.HOME.route,
            modifier = Modifier.fillMaxSize().padding(bottom = padding.calculateBottomPadding(), top = if (tab == null && route != Routes.ONBOARDING) padding.calculateTopPadding() else androidx.compose.ui.unit.Dp(0f)),
        ) {
            composable(Routes.ONBOARDING) {
                val vm: SettingsViewModel = hiltViewModel()
                OnboardingRoute(vm) {
                    nav.navigate(Tab.HOME.route) { popUpTo(Routes.ONBOARDING) { inclusive = true } }
                }
            }
            composable(Tab.HOME.route) { HomeRoute(onPickLocation = { nav.navigate(Routes.LOCATIONS) }) }
            composable(Tab.CALENDAR.route) { CalendarRoute() }
            composable(Tab.QIBLA.route) { QiblaRoute() }
            composable(Tab.TASBEH.route) { TasbehRoute() }
            composable(Tab.SETTINGS.route) {
                SettingsRoute(
                    hiltViewModel(),
                    onLocations = { nav.navigate(Routes.LOCATIONS) },
                    onNotifications = { nav.navigate(Routes.NOTIFICATIONS) },
                    onHelp = { nav.navigate(Routes.HELP) },
                )
            }
            composable(Routes.LOCATIONS) { LocationsScreen(hiltViewModel(), onPicked = { nav.popBackStack() }) }
            composable(Routes.NOTIFICATIONS) { NotificationSettingsRoute(hiltViewModel()) }
            composable(Routes.HELP) { HelpScreen() }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SubScreenBar(route: String?, nav: NavHostController) {
    val title = when (route) {
        Routes.LOCATIONS -> R.string.saved_locations
        Routes.NOTIFICATIONS -> R.string.notifications
        Routes.HELP -> R.string.help_notifications
        else -> return
    }
    TopAppBar(
        title = { Text(stringResource(title)) },
        navigationIcon = {
            IconButton(onClick = { nav.popBackStack() }) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, stringResource(R.string.back)) }
        },
    )
}
