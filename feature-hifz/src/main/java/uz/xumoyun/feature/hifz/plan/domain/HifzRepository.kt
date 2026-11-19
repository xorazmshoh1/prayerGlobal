package uz.xumoyun.feature.hifz.plan.domain

import java.time.LocalDate
import kotlinx.coroutines.flow.Flow

/**
 * Hifz ma'lumotlari (user.db, NamozDatabase). Implementatsiya — plan/data (Room). ViewModel'lar shu interfeysga
 * bog'lanadi, shuning uchun ularni Android'siz fake bilan sinash mumkin.
 */
interface HifzRepository {
    fun observeActivePlan(): Flow<HifzPlan?>

    fun observeProgress(): Flow<Map<Int, AyahProgress>>

    /** [from]..[to] (ikkalasi ham kiradi) oralig'idagi seanslar. */
    suspend fun sessionsBetween(from: LocalDate, to: LocalDate): List<ReviewSession>

    /** Yangi faol reja; oldingi faol rejalar o'chiriladi (faqat bittasi faol). Progress saqlanadi. */
    suspend fun activatePlan(plan: HifzPlan)

    suspend fun saveProgress(rows: List<AyahProgress>)

    suspend fun saveSessions(sessions: List<ReviewSession>)
}
