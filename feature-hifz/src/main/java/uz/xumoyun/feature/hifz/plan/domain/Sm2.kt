package uz.xumoyun.feature.hifz.plan.domain

import java.time.Instant
import java.time.LocalDate
import kotlin.math.max
import kotlin.math.roundToInt

/** Baho (TZ 7-bo'lim): oson = 5, qiyin = 3, unutdim = 1. */
object Quality {
    const val EASY = 5
    const val HARD = 3
    const val FORGOT = 1
}

/** SM-2 holati: easeFactor (EF), intervalDays, repetitions (ketma-ket to'g'ri takrorlar). */
data class Sm2Card(val easeFactor: Double, val intervalDays: Int, val repetitions: Int) {
    companion object {
        /** Standart SM-2 boshlang'ich EF. TZ aniq ko'rsatmagan; test qiymatlari shu bilan mos. */
        const val INITIAL_EASE = 2.5
        const val MIN_EASE = 1.3
        val NEW = Sm2Card(INITIAL_EASE, 0, 0)
    }
}

/** Oyat MEMORIZED bo'lishi uchun: interval >= shu kun VA oxirgi baho >= [MEMORIZED_MIN_QUALITY]. */
const val MEMORIZED_MIN_INTERVAL_DAYS = 30
const val MEMORIZED_MIN_QUALITY = 4

/**
 * SM-2 (TZ 7-bo'lim, soddalashtirilgan):
 * EF' = max(1.3, EF + 0.1 − (5 − q)(0.08 + 0.02(5 − q))).
 * q < 3 bo'lsa: repetitions = 0, interval = 1 kun. Aks holda 1-takror → 1 kun, 2-takror → 6 kun,
 * keyin interval = round(oldingi interval × EF').
 */
fun sm2Next(card: Sm2Card, quality: Int): Sm2Card {
    require(quality in 0..5) { "baho 0..5 bo'lishi kerak: $quality" }
    val d = 5 - quality
    val ef = max(Sm2Card.MIN_EASE, card.easeFactor + 0.1 - d * (0.08 + 0.02 * d))
    if (quality < 3) return Sm2Card(ef, 1, 0)
    val repetitions = card.repetitions + 1
    val interval = when (repetitions) {
        1 -> 1
        2 -> 6
        else -> max(1, (card.intervalDays * ef).roundToInt())
    }
    return Sm2Card(ef, interval, repetitions)
}

/**
 * Bitta oyatni baholagandan keyingi holati. [previous] null bo'lsa oyat hali o'rganilmagan (SM-2 boshlang'ich holati).
 * Keyingi takror sanasi = [today] + yangi interval. [now] — baholash vaqti (lastReviewedAt).
 */
fun reviewAyah(
    ayahGlobalId: Int,
    previous: AyahProgress?,
    quality: Int,
    today: LocalDate,
    now: Instant,
): AyahProgress {
    require(previous == null || previous.ayahGlobalId == ayahGlobalId) { "oyat raqami mos emas" }
    val base = previous?.let { Sm2Card(it.easeFactor, it.intervalDays, it.repetitions) } ?: Sm2Card.NEW
    val next = sm2Next(base, quality)
    val state = if (next.intervalDays >= MEMORIZED_MIN_INTERVAL_DAYS && quality >= MEMORIZED_MIN_QUALITY) {
        MemoState.MEMORIZED
    } else {
        MemoState.LEARNING
    }
    return AyahProgress(
        ayahGlobalId = ayahGlobalId,
        state = state,
        easeFactor = next.easeFactor,
        intervalDays = next.intervalDays,
        repetitions = next.repetitions,
        dueDate = today.plusDays(next.intervalDays.toLong()),
        lastRating = quality,
        lastReviewedAt = now,
    )
}
