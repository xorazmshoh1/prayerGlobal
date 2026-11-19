package uz.xumoyun.feature.hifz

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import uz.xumoyun.feature.hifz.ui.MushafRoute
import uz.xumoyun.feature.hifz.ui.ReaderRoute

/** Mus'haf marshrutlari. Argument nomlari ReaderViewModel dagi SavedStateHandle kalitlari bilan bir xil. */
object MushafRoutes {
    const val HOME = "mushaf"
    const val SURAH = "mushaf/surah/{surahId}"
    const val JUZ = "mushaf/juz/{juz}"

    fun surah(surahId: Int) = "mushaf/surah/$surahId"
    fun juz(juz: Int) = "mushaf/juz/$juz"
}

/** Ilova NavHost ichiga ulanadi: `mushafGraph(nav)`. Mus'haf ekranlari o'z yuqori panelini chizadi. */
fun NavGraphBuilder.mushafGraph(navController: NavController) {
    composable(MushafRoutes.HOME) {
        MushafRoute(
            onBack = { navController.popBackStack() },
            onSurah = { navController.navigate(MushafRoutes.surah(it)) },
            onJuz = { navController.navigate(MushafRoutes.juz(it)) },
        )
    }
    composable(
        MushafRoutes.SURAH,
        arguments = listOf(navArgument("surahId") { type = NavType.IntType }),
    ) {
        ReaderRoute(onBack = { navController.popBackStack() })
    }
    composable(
        MushafRoutes.JUZ,
        arguments = listOf(navArgument("juz") { type = NavType.IntType }),
    ) {
        ReaderRoute(onBack = { navController.popBackStack() })
    }
}
