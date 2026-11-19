package uz.xumoyun.feature.hifz.audio

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import uz.xumoyun.feature.hifz.audio.ui.AudioManagementRoute

object AudioRoutes {
    const val HOME = "audio"
}

/** Ilova NavHost ichiga ulanadi: `audioGraph(nav)`. */
fun NavGraphBuilder.audioGraph(navController: NavController) {
    composable(AudioRoutes.HOME) {
        AudioManagementRoute(onBack = { navController.popBackStack() })
    }
}
