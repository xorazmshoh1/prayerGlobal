package uz.xumoyun.feature.prayer.tracker.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import uz.xumoyun.feature.prayer.data.PrayerRepository
import uz.xumoyun.feature.prayer.domain.AsrMadhab
import uz.xumoyun.feature.prayer.tracker.domain.MadhabSource
import javax.inject.Inject

/** Joriy joy konfiguratsiyasidagi mazhab (qo'lda yoki avtomatik). Joy tanlanmagan bo'lsa Shofeiy. */
class PrayerMadhabSource @Inject constructor(private val prayers: PrayerRepository) : MadhabSource {
    override fun madhab(): Flow<AsrMadhab> =
        prayers.config.map { it?.input?.madhab ?: AsrMadhab.SHAFI }.distinctUntilChanged()
}
