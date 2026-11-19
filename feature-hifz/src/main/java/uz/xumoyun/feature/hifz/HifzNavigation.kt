package uz.xumoyun.feature.hifz

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import uz.xumoyun.feature.hifz.ui.lesson.LessonRoute
import uz.xumoyun.feature.hifz.ui.plan.PlanRoute
import uz.xumoyun.feature.hifz.ui.progress.ProgressRoute

/** Hifz marshrutlari (Hifz tabining o'zi NamozRoot'da). */
object HifzRoutes {
    const val LESSON = "hifz/lesson?type={type}"
    const val PLAN = "hifz/plan"
    const val PROGRESS = "hifz/progress"

    /** type: "SABAQ", "SABQI", "MANZIL" yoki null (hammasi). */
    fun lesson(type: String?) = if (type == null) "hifz/lesson" else "hifz/lesson?type=$type"
}

/** Ilova NavHost ichiga ulanadi: `hifzGraph(nav)`. Ekranlar o'z yuqori panelini chizadi. */
fun NavGraphBuilder.hifzGraph(navController: NavController) {
    composable(
        HifzRoutes.LESSON,
        arguments = listOf(navArgument("type") { type = NavType.StringType; nullable = true; defaultValue = null }),
    ) {
        LessonRoute(onDone = { navController.popBackStack() })
    }
    composable(HifzRoutes.PLAN) {
        PlanRoute(onBack = { navController.popBackStack() })
    }
    composable(HifzRoutes.PROGRESS) {
        ProgressRoute(onBack = { navController.popBackStack() })
    }
}
