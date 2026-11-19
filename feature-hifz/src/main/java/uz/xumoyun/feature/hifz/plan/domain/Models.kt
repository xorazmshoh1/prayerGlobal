package uz.xumoyun.feature.hifz.plan.domain

import java.time.Instant
import java.time.LocalDate

/** Qur'on oyatlari soni (quran.db: globalId 1..6236). */
const val QURAN_AYAH_COUNT = 6236

/** Yodlash holati (TZ 7-bo'lim AyahProgress.state). */
enum class MemoState { NEW, LEARNING, MEMORIZED }

/** Reja maqsadi (TZ 7-bo'lim HifzPlan.scope). */
enum class PlanScope { SURAH, JUZ, ALL }

/** Dars turi (TZ 7-bo'lim ReviewSession.type). */
enum class ReviewType { SABAQ, SABQI, MANZIL }

/**
 * Bitta oyatning takrorlash holati (TZ 7-bo'lim AyahProgress).
 * [ayahGlobalId] — 1..6236; [lastRating] — so'nggi baho (1, 3 yoki 5), hali baholanmagan bo'lsa null.
 */
data class AyahProgress(
    val ayahGlobalId: Int,
    val state: MemoState,
    val easeFactor: Double,
    val intervalDays: Int,
    val repetitions: Int,
    val dueDate: LocalDate,
    val lastRating: Int?,
    val lastReviewedAt: Instant?,
)

/**
 * Hifz rejasi (TZ 7-bo'lim HifzPlan). [fromAyah]..[toAyah] — GLOBAL oyat raqamlari (1..6236), ya'ni
 * sura/juz tanlanganda chegaralarni chaqiruvchi (quran.db dan) hisoblaydi; shu sababli domain
 * faqat oraliqni biladi. [reminderAfterPrayer] — namoz kaliti (masalan "FAJR") yoki null.
 */
data class HifzPlan(
    val id: Long = 0,
    val scope: PlanScope,
    val fromAyah: Int,
    val toAyah: Int,
    val dailyNewAyahs: Int,
    val reminderAfterPrayer: String?,
    val reminderOffsetMin: Int,
    val startDate: LocalDate,
    val isActive: Boolean,
) {
    init {
        require(fromAyah in 1..QURAN_AYAH_COUNT) { "boshlang'ich oyat: $fromAyah" }
        require(toAyah in fromAyah..QURAN_AYAH_COUNT) { "oxirgi oyat: $toAyah" }
        require(dailyNewAyahs >= 1) { "kunlik yangi oyatlar: $dailyNewAyahs" }
        require(reminderOffsetMin >= 0) { "eslatma oralig'i: $reminderOffsetMin" }
    }

    /** Rejadagi oyatlar, tartib bo'yicha (global id). */
    val ayahIds: List<Int> get() = (fromAyah..toAyah).toList()
}

/**
 * Bajarilgan dars seansi (TZ 7-bo'lim ReviewSession). [ayahFrom]..[ayahTo] — global oyat raqamlari, ikkalasi ham kiradi.
 */
data class ReviewSession(
    val id: Long = 0,
    val date: LocalDate,
    val type: ReviewType,
    val ayahFrom: Int,
    val ayahTo: Int,
    val durationSec: Int,
    val avgRating: Double?,
) {
    init {
        require(ayahFrom in 1..QURAN_AYAH_COUNT && ayahTo in ayahFrom..QURAN_AYAH_COUNT) {
            "oyat oralig'i: $ayahFrom..$ayahTo"
        }
        require(durationSec >= 0) { "davomiylik: $durationSec" }
    }
}
