package uz.xumoyun.namoz.review

import android.app.Activity
import com.google.android.play.core.review.ReviewManagerFactory
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.first
import uz.xumoyun.core.common.TimeSource
import uz.xumoyun.core.common.hijri.HijriCalendar
import uz.xumoyun.core.common.review.ReviewPolicy
import uz.xumoyun.core.data.CommonSettings
import uz.xumoyun.feature.hifz.plan.domain.HifzRepository
import uz.xumoyun.feature.hifz.plan.domain.streakDays
import uz.xumoyun.feature.ramadan.domain.RamadanCalc

/**
 * In-App Review (TZ 12-bo'lim). Shart [ReviewPolicy] da (sof, testlangan): 7 kunlik hifz seriyasi yoki Ramazonning
 * 10-kuni, 30 kunda bir marta. So'rov tugma orqali emas — ilova ochilganda qiymat bo'lsa.
 */
@Singleton
class ReviewPrompter @Inject constructor(
    private val common: CommonSettings,
    private val hifz: HifzRepository,
    private val hijri: uz.xumoyun.core.data.HijriProvider,
    private val time: TimeSource,
) {
    suspend fun maybePrompt(activity: Activity) {
        val today = time.today()
        val last = common.lastReviewRequestDay.first()?.let { LocalDate.ofEpochDay(it) }
        val streak = streakDays(today, hifz.sessionsBetween(today.minusDays(60), today).map { it.date }.toSet())
        val offset = common.hijriAdjustDays.first()
        val ramadanDay = RamadanCalc.upcomingStart(today, hijri.calendar(offset))?.let { RamadanCalc.dayNumber(today, it) }
        if (!ReviewPolicy.shouldRequest(today, last, streak, ramadanDay)) return

        common.setLastReviewRequestDay(today.toEpochDay())
        val manager = ReviewManagerFactory.create(activity)
        manager.requestReviewFlow().addOnCompleteListener { task ->
            if (task.isSuccessful) manager.launchReviewFlow(activity, task.result)
        }
    }
}
