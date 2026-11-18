package uz.xumoyun.feature.ramadan

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import uz.xumoyun.core.ui.components.EmptyState

/** Ramazon navigatsiya grafi marshrutlari. R2–R6 ekranlari 11-bosqichda qo'shiladi. */
object RamadanRoutes {
    const val GRAPH = "ramadan"
    const val HOME = "ramadan/home"
}

/**
 * R1 — Ramazon bosh sahifasi. 1-bosqichda bo'sh ekran; saharlik/iftor kartalari, Ramazon kuni
 * va tez havolalar 11-bosqichda (TZ 6 va 11-bo'limlar).
 */
@Composable
fun RamadanHomeRoute(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        EmptyState(
            icon = Icons.Rounded.DarkMode,
            title = stringResource(R.string.ramadan_title),
            message = stringResource(R.string.ramadan_coming_soon),
        )
    }
}
