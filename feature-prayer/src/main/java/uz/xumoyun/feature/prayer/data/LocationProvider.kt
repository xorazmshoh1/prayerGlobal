package uz.xumoyun.feature.prayer.data

import android.location.Location
import uz.xumoyun.feature.prayer.domain.City

/** Joylashuv manbai (testlarda soxta versiya). Haqiqiysi — [AndroidLocationProvider]. */
interface LocationProvider {
    /** ACCESS_COARSE_LOCATION berilganmi. */
    fun hasPermission(): Boolean
    suspend fun currentLocation(): Location?
    fun toCity(location: Location): City
}
