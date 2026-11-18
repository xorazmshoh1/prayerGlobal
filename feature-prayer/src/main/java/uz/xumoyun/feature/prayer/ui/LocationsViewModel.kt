package uz.xumoyun.feature.prayer.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import uz.xumoyun.feature.prayer.data.CityRepository
import uz.xumoyun.feature.prayer.data.LocationLabel
import uz.xumoyun.feature.prayer.data.LocationProvider
import uz.xumoyun.feature.prayer.data.PrayerRepository
import uz.xumoyun.feature.prayer.data.SavedLocation
import uz.xumoyun.feature.prayer.domain.City
import javax.inject.Inject

/** GPS natijasi: joy topildi, ruxsat rad etildi (shahar qidiruviga o'tiladi) yoki joy aniqlanmadi. */
enum class GpsResult { FOUND, DENIED, FAILED }

data class LocationsState(
    val saved: List<SavedLocation> = emptyList(),
    val selectedId: Long? = null,
    val query: String = "",
    val results: List<City> = emptyList(),
    val locating: Boolean = false,
    val gps: GpsResult? = null,
)

/** Joylar ekrani va onboarding'dagi joy qadami: saqlangan joylar, GPS (COARSE) va GeoNames shahar qidiruvi. */
@HiltViewModel
class LocationsViewModel @Inject constructor(
    private val repository: PrayerRepository,
    private val cities: CityRepository,
    private val locationProvider: LocationProvider,
) : ViewModel() {

    /** Qidiruv va GPS holati (saqlangan joylar repozitoriyadan qo'shiladi). */
    private val ui = MutableStateFlow(LocationsState())

    val state: StateFlow<LocationsState> = combine(repository.savedLocations, repository.config, ui) { saved, config, u ->
        u.copy(saved = saved, selectedId = config?.location?.id)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), LocationsState())

    init { search("") }

    fun search(query: String) {
        ui.update { it.copy(query = query, results = cities.index.search(query)) }
    }

    fun addCity(city: City, label: LocationLabel) = viewModelScope.launch { repository.addAndSelect(city, label) }

    /** Ruxsat so'rovi natijasi kelgandan keyin chaqiriladi. */
    fun onPermissionResult(granted: Boolean, label: LocationLabel, onFound: () -> Unit = {}) {
        if (!granted || !locationProvider.hasPermission()) {
            ui.update { it.copy(gps = GpsResult.DENIED) }
            return
        }
        useGps(label, onFound)
    }

    fun useGps(label: LocationLabel, onFound: () -> Unit = {}) = viewModelScope.launch {
        ui.update { it.copy(locating = true, gps = null) }
        val loc = locationProvider.currentLocation()
        if (loc != null) repository.addAndSelect(locationProvider.toCity(loc), label, fromGps = true)
        ui.update { it.copy(locating = false, gps = if (loc != null) GpsResult.FOUND else GpsResult.FAILED) }
        if (loc != null) onFound()
    }

    fun select(id: Long) = viewModelScope.launch { repository.select(id) }
    fun delete(id: Long) = viewModelScope.launch { repository.delete(id) }
}
