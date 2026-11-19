package uz.xumoyun.feature.hifz.plan.data

import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import uz.xumoyun.feature.hifz.plan.domain.AyahProgress
import uz.xumoyun.feature.hifz.plan.domain.HifzPlan
import uz.xumoyun.feature.hifz.plan.domain.HifzRepository
import uz.xumoyun.feature.hifz.plan.domain.ReviewSession

/** [HifzRepository] ning Room (NamozDatabase v4) amalga oshirishi. */
@Singleton
class RoomHifzRepository @Inject constructor(private val dao: HifzDao) : HifzRepository {

    override fun observeActivePlan(): Flow<HifzPlan?> =
        dao.observeActivePlan().map { it?.toDomain() }

    override fun observeProgress(): Flow<Map<Int, AyahProgress>> =
        dao.observeProgress().map { rows -> rows.associate { it.ayahGlobalId to it.toDomain() } }

    override suspend fun sessionsBetween(from: LocalDate, to: LocalDate): List<ReviewSession> =
        dao.sessionsBetween(from.toEpochDay(), to.toEpochDay()).map { it.toDomain() }

    override suspend fun activatePlan(plan: HifzPlan) {
        dao.deactivateAllPlans()
        dao.insertPlan(plan.copy(isActive = true).toEntity())
    }

    override suspend fun saveProgress(rows: List<AyahProgress>) {
        if (rows.isEmpty()) return
        dao.upsertProgress(rows.map { it.toEntity() })
    }

    override suspend fun saveSessions(sessions: List<ReviewSession>) {
        for (s in sessions) dao.insertSession(s.toEntity())
    }
}
