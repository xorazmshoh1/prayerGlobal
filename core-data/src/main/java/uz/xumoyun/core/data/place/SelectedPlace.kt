package uz.xumoyun.core.data.place

import kotlinx.coroutines.flow.Flow

/** Tanlangan joy (shahar) koordinatalari: qibla va boshqa feature'lar uchun. */
data class SelectedPlace(val name: String, val latitude: Double, val longitude: Double)

/**
 * Tanlangan joy manbai. Feature modullar bir-biriga bog'lanmasligi uchun interfeys shu yerda;
 * amaliy realizatsiya ilova modulida (feature-prayer'dagi repozitoriydan).
 * null — joy hali tanlanmagan.
 */
interface SelectedPlaceSource {
    val place: Flow<SelectedPlace?>
}
