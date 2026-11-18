package uz.xumoyun.namoz.ui.home

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
import uz.xumoyun.namoz.data.City
import uz.xumoyun.namoz.data.Geo
import uz.xumoyun.namoz.data.HijriProvider
import uz.xumoyun.namoz.data.LocationConfig
import uz.xumoyun.namoz.data.LocationProvider
import uz.xumoyun.namoz.data.NamozSettings
import uz.xumoyun.namoz.data.PrayerRepository
import uz.xumoyun.namoz.data.db.LocationLabel
import uz.xumoyun.namoz.domain.DayPhase
import uz.xumoyun.namoz.domain.DayTimes
import uz.xumoyun.namoz.domain.NextEvent
import uz.xumoyun.namoz.domain.Prayer
import uz.xumoyun.namoz.domain.Schedule
import java.time.LocalDate
import java.time.ZonedDateTime
import javax.inject.Inject

data class PrayerRow(val prayer: Prayer, val time: ZonedDateTime, val isCurrent: Boolean, val alertOn: Boolean)

sealed interface HomeState {
    data object Loading : HomeState
    data object NoLocation : HomeState
    data class Ready(
        val city: String,
        val today: LocalDate,
        val hijri: String,
        val rows: List<PrayerRow>,
        val next: NextEvent,
        val phase: DayPhase,
    ) : HomeState
}

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: PrayerRepository,
    private val settings: NamozSettings,
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

    private var cache: Pair<LocalDate, Pair<DayTimes, DayTimes>>? = null
    private var cacheKey: Any? = null

    val state: StateFlow<HomeState> = combine(repository.config, ticker) { config, _ ->
        if (config == null) HomeState.NoLocation else build(config)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), HomeState.Loading)

    private val _travel = MutableStateFlow<City?>(null)
    /** Safarda yangi joy aniqlansa — "Vaqtlarni yangi joyga moslaymi?" */
    val travelSuggestion: StateFlow<City?> = _travel.asStateFlow()

    internal fun build(config: LocationConfig): HomeState.Ready {
        val now = time.now().atZone(config.zone)
        val date = now.toLocalDate()
        val key = config.input
        val days = cache?.takeIf { it.first == date && cacheKey == key }?.second
            ?: (config.day(date) to config.day(date.plusDays(1))).also { cache = date to it; cacheKey = key }
        val (today, tomorrow) = days
        val current = Schedule.current(now, today)
        val next = Schedule.next(now, today, tomorrow)
        return HomeState.Ready(
            city = config.location.name,
            today = date,
            hijri = hijri.of(date, config.prefs.hijriOffset).formatted,
            rows = today.ordered.map { (p, t) ->
                PrayerRow(p, t, isCurrent = p == current && !now.isBefore(today[Prayer.FAJR]), alertOn = config.prefs.alerts.getValue(p).enabled)
            },
            next = next,
            phase = Schedule.phase(now, today),
        )
    }

    fun toggleAlert(prayer: Prayer) = viewModelScope.launch {
        val alert = settings.current().alerts.getValue(prayer)
        settings.setAlert(prayer, alert.copy(enabled = !alert.enabled))
    }

    /** Ilova ochilganda: GPS ruxsati bo'lsa, joy 50 km dan ko'p o'zgarganini tekshiradi. */
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

    private companion object {
        const val TRAVEL_KM = 50.0
    }
}
