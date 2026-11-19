package uz.xumoyun.feature.hifz.ui

import java.time.LocalDate
import kotlinx.coroutines.flow.MutableStateFlow
import uz.xumoyun.feature.hifz.plan.domain.AyahProgress
import uz.xumoyun.feature.hifz.plan.domain.HifzPlan
import uz.xumoyun.feature.hifz.plan.domain.HifzRepository
import uz.xumoyun.feature.hifz.plan.domain.ReviewSession

/** Xotirada ishlaydigan HifzRepository; yozuvlar ro'yxatlari testda tekshiriladi. */
class FakeHifzRepository(initialPlan: HifzPlan? = null) : HifzRepository {
    val plan = MutableStateFlow(initialPlan)
    val progress = MutableStateFlow<Map<Int, AyahProgress>>(emptyMap())
    val sessions = mutableListOf<ReviewSession>()

    val activated = mutableListOf<HifzPlan>()
    val savedProgress = mutableListOf<AyahProgress>()
    val savedSessions = mutableListOf<ReviewSession>()

    override fun observeActivePlan() = plan

    override fun observeProgress() = progress

    override suspend fun sessionsBetween(from: LocalDate, to: LocalDate): List<ReviewSession> =
        sessions.filter { !it.date.isBefore(from) && !it.date.isAfter(to) }

    override suspend fun activatePlan(plan: HifzPlan) {
        activated += plan
        this.plan.value = plan
    }

    override suspend fun saveProgress(rows: List<AyahProgress>) {
        savedProgress += rows
        progress.value = progress.value + rows.associateBy { it.ayahGlobalId }
    }

    override suspend fun saveSessions(sessions: List<ReviewSession>) {
        savedSessions += sessions
        this.sessions += sessions
    }
}

/** Dars pleyeri fake: chaqiruvlar yoziladi; [playResult] false bo'lsa fayl yo'q deb hisoblanadi. */
class FakeLessonAudio(var playResult: Boolean = true) : uz.xumoyun.feature.hifz.ui.lesson.LessonAudio {
    val plays = mutableListOf<List<Any>>()
    var speed: Float? = null
    var paused = 0
    var released = false

    override fun play(surah: Int, ayah: Int, times: Int, speed: Float): Boolean {
        plays += listOf(surah, ayah, times, speed)
        return playResult
    }

    override fun setSpeed(speed: Float) {
        this.speed = speed
    }

    override fun pause() {
        paused++
    }

    override fun release() {
        released = true
    }
}

/** Seans yaratuvchi yordamchi (testlarda qisqa yozuv uchun). */
fun reviewSession(
    date: java.time.LocalDate,
    type: uz.xumoyun.feature.hifz.plan.domain.ReviewType,
    from: Int,
    to: Int,
    dur: Int = 0,
    avg: Double? = null,
) = ReviewSession(date = date, type = type, ayahFrom = from, ayahTo = to, durationSec = dur, avgRating = avg)
