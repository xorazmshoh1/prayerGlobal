package uz.xumoyun.feature.ramadan.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.room.Dao
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import uz.xumoyun.feature.ramadan.domain.FastCalc
import uz.xumoyun.feature.ramadan.domain.FastStatus
import uz.xumoyun.feature.ramadan.domain.RamadanQadaPort
import uz.xumoyun.feature.ramadan.domain.RamadanSettings
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

/** Ro'za kuzatuvchisi: bitta kun uchun bitta yozuv (TZ 7-bo'lim, FastLog). */
@Entity(tableName = "fast_log")
data class FastLogEntity(
    @PrimaryKey val date: String,
    val hijriYear: Int,
    val hijriDay: Int,
    val status: String,
)

/** Xatm rejasi: har Ramazon uchun bitta yozuv (TZ 7-bo'lim, KhatmPlan). */
@Entity(tableName = "khatm_plan")
data class KhatmPlanEntity(
    @PrimaryKey val hijriYear: Int,
    val startDate: String,
    val juzDone: Int,
    val lastReadAyah: Int,
    val updatedAt: Long,
)

@Dao
interface RamadanDao {
    @Query("SELECT * FROM fast_log WHERE date BETWEEN :from AND :to")
    fun observeFasts(from: String, to: String): Flow<List<FastLogEntity>>

    @Query("SELECT * FROM fast_log WHERE date = :date LIMIT 1")
    suspend fun fast(date: String): FastLogEntity?

    @Query("DELETE FROM fast_log WHERE date = :date")
    suspend fun deleteFast(date: String)

    @Upsert
    suspend fun upsertFast(entity: FastLogEntity)

    @Query("SELECT * FROM khatm_plan WHERE hijriYear = :year LIMIT 1")
    fun observeKhatm(year: Int): Flow<KhatmPlanEntity?>

    @Upsert
    suspend fun upsertKhatm(entity: KhatmPlanEntity)
}

/** Ramazon sozlamalari (DataStore). Standart qiymatlar TZ bo'yicha. */
@Singleton
class RamadanPrefs @Inject constructor(private val store: DataStore<Preferences>) {
    val settings: Flow<RamadanSettings> = store.data.map { p ->
        val d = RamadanSettings()
        RamadanSettings(
            imsakOffsetMin = (p[IMSAK] ?: d.imsakOffsetMin).coerceIn(0, RamadanSettings.IMSAK_OFFSET_MAX),
            suhoorOn = p[SUHOOR_ON] ?: d.suhoorOn,
            suhoorMinBefore = (p[SUHOOR_MIN] ?: d.suhoorMinBefore).coerceIn(RamadanSettings.SUHOOR_MIN_MIN, RamadanSettings.SUHOOR_MIN_MAX),
            iftarOn = p[IFTAR_ON] ?: d.iftarOn,
            iftarMinBefore = (p[IFTAR_MIN] ?: d.iftarMinBefore).coerceIn(0, RamadanSettings.IFTAR_MIN_MAX),
            qadrOn = p[QADR_ON] ?: d.qadrOn,
        )
    }.distinctUntilChanged()

    suspend fun update(transform: (RamadanSettings) -> RamadanSettings) {
        val next = transform(settings.first())
        store.edit {
            it[IMSAK] = next.imsakOffsetMin
            it[SUHOOR_ON] = next.suhoorOn
            it[SUHOOR_MIN] = next.suhoorMinBefore
            it[IFTAR_ON] = next.iftarOn
            it[IFTAR_MIN] = next.iftarMinBefore
            it[QADR_ON] = next.qadrOn
        }
    }

    private companion object {
        val IMSAK = intPreferencesKey("ramadan_imsak_offset")
        val SUHOOR_ON = booleanPreferencesKey("ramadan_suhoor_on")
        val SUHOOR_MIN = intPreferencesKey("ramadan_suhoor_min")
        val IFTAR_ON = booleanPreferencesKey("ramadan_iftar_on")
        val IFTAR_MIN = intPreferencesKey("ramadan_iftar_min")
        val QADR_ON = booleanPreferencesKey("ramadan_qadr_on")
    }
}

/** Ramazon ma'lumotlari: ro'za holati (Room) + qazo (port orqali feature-prayer'ga). */
@Singleton
class RamadanRepository @Inject constructor(
    private val dao: RamadanDao,
    private val qada: RamadanQadaPort,
) {
    fun fasts(start: LocalDate): Flow<Map<LocalDate, FastStatus>> =
        dao.observeFasts(start.toString(), start.plusDays(29).toString()).map { rows ->
            rows.associate { LocalDate.parse(it.date) to FastStatus.valueOf(it.status) }
        }

    fun fastQada(): Flow<Int> = qada.fastQada()

    /**
     * Ro'za holatini belgilaydi (null — belgisiz). MISSED ga o'tganda ro'za qazosiga +1, qaytarilganda −1
     * ([FastCalc.qadaDelta]). Bir xil holat qayta belgilansa qazo o'zgarmaydi.
     */
    suspend fun setFast(date: LocalDate, hijriYear: Int, hijriDay: Int, status: FastStatus?) {
        val old = dao.fast(date.toString())?.let { FastStatus.valueOf(it.status) }
        if (status == null) dao.deleteFast(date.toString())
        else dao.upsertFast(FastLogEntity(date.toString(), hijriYear, hijriDay, status.name))
        val delta = FastCalc.qadaDelta(old, status)
        if (delta != 0) qada.adjustFast(delta)
    }

    fun khatm(hijriYear: Int): Flow<KhatmPlanEntity?> = dao.observeKhatm(hijriYear)

    suspend fun setJuzDone(hijriYear: Int, start: LocalDate, juzDone: Int, lastReadAyah: Int, now: Long) {
        dao.upsertKhatm(KhatmPlanEntity(hijriYear, start.toString(), juzDone.coerceIn(0, 30), lastReadAyah, now))
    }
}
