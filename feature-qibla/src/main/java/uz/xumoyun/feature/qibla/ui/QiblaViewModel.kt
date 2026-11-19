package uz.xumoyun.feature.qibla.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import uz.xumoyun.core.common.geo.Geo
import uz.xumoyun.core.data.place.SelectedPlace
import uz.xumoyun.core.data.place.SelectedPlaceSource
import uz.xumoyun.feature.qibla.data.CompassSource
import uz.xumoyun.feature.qibla.domain.QiblaTracker
import javax.inject.Inject

/** Qibla yo'nalishi: shahar nomi, azimut (shimoldan), Ka'bagacha masofa (km). */
data class QiblaTarget(val city: String, val bearing: Double, val distanceKm: Double)

data class QiblaUiState(
    /** Joy hali o'qilmagan. */
    val loading: Boolean = true,
    /** null — joy tanlanmagan (Namoz tabida shahar tanlash kerak). */
    val target: QiblaTarget? = null,
    val hasCompass: Boolean = true,
    /** Haqiqiy shimol azimuti; birinchi o'qishgacha null. */
    val headingDeg: Double? = null,
    /** Siferblatning uzluksiz aylanishi (gradus). */
    val dialRotationDeg: Float = 0f,
    /** Strelka qiblaga tekis. */
    val aligned: Boolean = false,
    val needsCalibration: Boolean = false,
)

@HiltViewModel
class QiblaViewModel @Inject constructor(
    place: SelectedPlaceSource,
    private val compass: CompassSource,
) : ViewModel() {

    private val alignedSignal = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

    /** Strelka qiblaga yangi kirganda bir marta chiqadi (UI shu yerda qisqa tebranadi, TZ 6). */
    val alignedEvents: SharedFlow<Unit> = alignedSignal.asSharedFlow()

    val state: StateFlow<QiblaUiState> = place.place
        .flatMapLatest { selected ->
            if (selected == null) flowOf(QiblaUiState(loading = false)) else track(selected)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), QiblaUiState())

    private fun track(selected: SelectedPlace): Flow<QiblaUiState> {
        val bearing = Geo.qiblaBearing(selected.latitude, selected.longitude)
        val target = QiblaTarget(
            city = selected.name,
            bearing = bearing,
            distanceKm = Geo.qiblaDistanceKm(selected.latitude, selected.longitude),
        )
        val hasCompass = compass.isAvailable()
        val initial = QiblaUiState(loading = false, target = target, hasCompass = hasCompass)
        if (!hasCompass) return flowOf(initial)

        val tracker = QiblaTracker(bearing)
        return compass.readings(selected.latitude, selected.longitude)
            .map { reading ->
                if (tracker.onReading(reading.azimuth, reading.accuracy)) alignedSignal.tryEmit(Unit)
                QiblaUiState(
                    loading = false,
                    target = target,
                    hasCompass = true,
                    headingDeg = tracker.headingDeg,
                    dialRotationDeg = tracker.dialRotationDeg.toFloat(),
                    aligned = tracker.aligned,
                    needsCalibration = tracker.needsCalibration,
                )
            }
            .onStart { emit(initial) }
    }
}
