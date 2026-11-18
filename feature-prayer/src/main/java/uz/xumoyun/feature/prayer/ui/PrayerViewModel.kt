package uz.xumoyun.feature.prayer.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import uz.xumoyun.core.common.TimeSource
import uz.xumoyun.core.data.CommonSettings
import uz.xumoyun.core.data.HijriProvider
import uz.xumoyun.feature.prayer.data.LocationConfig
import uz.xumoyun.feature.prayer.data.LocationLabel
import uz.xumoyun.feature.prayer.data.LocationProvider
import uz.xumoyun.feature.prayer.data.PrayerRepository
import uz.xumoyun.feature.prayer.data.PrayerSettings
import uz.xumoyun.feature.prayer.domain.City
import uz.xumoyun.feature.prayer.domain.DayTimes
import uz.xumoyun.feature.prayer.domain.Geo
import uz.xumoyun.feature.prayer.domain.Prayer
import uz.xumoyun.feature.prayer.domain.PrayerBoard
import java.time.LocalDate
import javax.inject.Inject

sealed interface PrayerState {
    data object Loading : PrayerState
    data object NoLocation : PrayerState
    data class Ready(
        val city: String,
        val today: LocalDate,
        val hijri: String,
        val board: PrayerBoard,
    ) : PrayerState
}

/** Namoz tabi (TZ 6-bo'lim, 2-ekran): shahar, ikki sana, keyingi namoz va teskari sanoq, 6 vaqt. */
@HiltViewModel
class PrayerViewModel @Inject constructor(
    private val repository: PrayerRepository,
    private val settings: PrayerSettings,
    private val common: CommonSettings,
    private val hijri: HijriProvider,
    private val locationProvider: LocationProvider,
    private val time: TimeSource,
) : ViewModel() {

    private val ticker: Flow<Unit> = flow {
        while (true) {
            emit(Unit)
            delay(1000)
        }
    }

    /** Bugun va ertangi vaqtlar: soniyada bir qayta hisoblamaslik uchun. */
    private var cache: Triple<LocalDate, Any, Pair<DayTimes, DayTimes>>? = null

    val state: StateFlow<PrayerState> = combine(repository.config, common.hijriAdjustDays, ticker) { config, hijriAdjust, _ ->
        if (config == null) PrayerState.NoLocation else build(config, hijriAdjust)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), PrayerState.Loading)

    private val _travel = MutableStateFlow<City?>(null)
    /** Safarda yangi joy aniqlansa — "Vaqtlarni yangi joyga moslaymi?" */
    val travelSuggestion: StateFlow<City?> = _travel.asStateFlow()

    internal fun build(config: LocationConfig, hijriAdjust: Int = 0): PrayerState.Ready {
        val now = time.now().atZone(config.zone)
        val date = now.toLocalDate()
        val days = cache?.takeIf { it.first == date && it.second == config.input }?.third
            ?: (config.day(date) to config.day(date.plusDays(1))).also { cache = Triple(date, config.input, it) }
        val (today, tomorrow) = days
        return PrayerState.Ready(
            city = config.location.name,
            today = date,
            hijri = hijri.of(date, hijriAdjust).formatted,
            board = PrayerBoard.build(now, today, tomorrow) { config.prefs.alerts.getValue(it).enabled },
        )
    }

    /** Qatordagi ovoz belgisi: shu namoz uchun bildirishnomani yoqish/o'chirish. */
    fun toggleAlert(prayer: Prayer) = viewModelScope.launch {
        val alert = settings.current().alerts.getValue(prayer)
        settings.setAlert(prayer, alert.copy(enabled = !alert.enabled))
    }

    /** Ilova ochilganda: joylashuv ruxsati bo'lsa, joy [TRAVEL_KM] dan ko'p o'zgarganini tekshiradi. */
    fun checkTravel() = viewModelScope.launch {
        if (!locationProvider.hasPermission()) return@launch
        val config = repository.currentConfig() ?: return@launch
        val loc = locationProvider.currentLocation() ?: return@launch
        val distance = Geo.distanceKm(loc.latitude, loc.longitude, config.location.latitude, config.location.longitude)
        if (distance > TRAVEL_KM) _travel.value = locationProvider.toCity(loc)
    }

    fun acceptTravel() = viewModelScope.launch {
        _travel.value?.let { repository.addAndSelect(it, LocationLabel.TRAVEL, fromGps = true) }
        _travel.value = null
    }

    fun dismissTravel() { _travel.value = null }

    companion object {
        const val TRAVEL_KM = 50.0
    }
}
