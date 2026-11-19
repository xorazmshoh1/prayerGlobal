package uz.xumoyun.feature.hifz.plan.data

import uz.xumoyun.feature.hifz.plan.domain.AyahProgress
import uz.xumoyun.feature.hifz.plan.domain.HifzPlan
import uz.xumoyun.feature.hifz.plan.domain.MemoState
import uz.xumoyun.feature.hifz.plan.domain.PlanScope
import uz.xumoyun.feature.hifz.plan.domain.ReviewSession
import uz.xumoyun.feature.hifz.plan.domain.ReviewType
import java.time.Instant
import java.time.LocalDate

/** Entity ↔ domen konvertatsiyasi (sof funksiyalar, unit test qilinadi). */

fun HifzPlanEntity.toDomain(): HifzPlan = HifzPlan(
    id = id,
    scope = PlanScope.valueOf(scope),
    fromAyah = fromAyah,
    toAyah = toAyah,
    dailyNewAyahs = dailyNewAyahs,
    reminderAfterPrayer = reminderAfterPrayer,
    reminderOffsetMin = reminderOffsetMin,
    startDate = LocalDate.ofEpochDay(startDate),
    isActive = isActive,
)

fun HifzPlan.toEntity(): HifzPlanEntity = HifzPlanEntity(
    id = id,
    scope = scope.name,
    fromAyah = fromAyah,
    toAyah = toAyah,
    dailyNewAyahs = dailyNewAyahs,
    reminderAfterPrayer = reminderAfterPrayer,
    reminderOffsetMin = reminderOffsetMin,
    startDate = startDate.toEpochDay(),
    isActive = isActive,
)

fun AyahProgressEntity.toDomain(): AyahProgress = AyahProgress(
    ayahGlobalId = ayahGlobalId,
    state = MemoState.valueOf(state),
    easeFactor = easeFactor,
    intervalDays = intervalDays,
    repetitions = repetitions,
    dueDate = LocalDate.ofEpochDay(dueDate),
    lastRating = lastRating,
    lastReviewedAt = lastReviewedAt?.let { Instant.ofEpochMilli(it) },
)

fun AyahProgress.toEntity(): AyahProgressEntity = AyahProgressEntity(
    ayahGlobalId = ayahGlobalId,
    state = state.name,
    easeFactor = easeFactor,
    intervalDays = intervalDays,
    repetitions = repetitions,
    dueDate = dueDate.toEpochDay(),
    lastRating = lastRating,
    lastReviewedAt = lastReviewedAt?.toEpochMilli(),
)

fun ReviewSessionEntity.toDomain(): ReviewSession = ReviewSession(
    id = id,
    date = LocalDate.ofEpochDay(date),
    type = ReviewType.valueOf(type),
    ayahFrom = ayahFrom,
    ayahTo = ayahTo,
    durationSec = durationSec,
    avgRating = avgRating,
)

fun ReviewSession.toEntity(): ReviewSessionEntity = ReviewSessionEntity(
    id = id,
    date = date.toEpochDay(),
    type = type.name,
    ayahFrom = ayahFrom,
    ayahTo = ayahTo,
    durationSec = durationSec,
    avgRating = avgRating,
)
