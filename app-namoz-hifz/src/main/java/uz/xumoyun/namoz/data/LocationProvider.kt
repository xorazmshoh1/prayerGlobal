package uz.xumoyun.namoz.data

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Looper
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

/** GPS (ixtiyoriy). Play Services talab qilinmaydi — oddiy LocationManager. */
@Singleton
class AndroidLocationProvider @Inject constructor(
    @ApplicationContext private val context: Context,
    private val cities: CityRepository,
) : LocationProvider {
    private val manager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager

    override fun hasPermission(): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

    /** Joriy koordinata yoki null (ruxsat yo'q, GPS o'chiq, 15 soniyada topilmadi). */
    @SuppressLint("MissingPermission")
    override suspend fun currentLocation(): Location? {
        if (!hasPermission()) return null
        val providers = manager.getProviders(true)
        val last = providers.mapNotNull { runCatching { manager.getLastKnownLocation(it) }.getOrNull() }
            .maxByOrNull { it.time }
        if (last != null && System.currentTimeMillis() - last.time < 30 * 60 * 1000L) return last
        // Faqat COARSE ruxsat so'raladi (TZ 8-bo'lim): GPS_PROVIDER FINE talab qiladi, shuning uchun tarmoq provayderi.
        val provider = LocationManager.NETWORK_PROVIDER.takeIf { it in providers } ?: return last
        return withTimeoutOrNull(15_000) {
            suspendCancellableCoroutine { cont ->
                val listener = object : LocationListener {
                    override fun onLocationChanged(location: Location) {
                        manager.removeUpdates(this)
                        if (cont.isActive) cont.resume(location)
                    }
                    @Deprecated("Deprecated in Java")
                    override fun onStatusChanged(provider: String?, status: Int, extras: android.os.Bundle?) = Unit
                    override fun onProviderEnabled(provider: String) = Unit
                    override fun onProviderDisabled(provider: String) = Unit
                }
                manager.requestLocationUpdates(provider, 0L, 0f, listener, Looper.getMainLooper())
                cont.invokeOnCancellation { manager.removeUpdates(listener) }
            }
        } ?: last
    }

    /**
     * Koordinatani shaharga aylantiradi: eng yaqin shahar nomi va davlati olinadi,
     * koordinatalar esa GPS dagidek qoladi.
     */
    override fun toCity(location: Location): City {
        val nearest = cities.index.nearest(location.latitude, location.longitude)
        val near = nearest?.takeIf { it.second < 150 }?.first
        return City(
            name = near?.name ?: String.format(java.util.Locale.ROOT, "%.3f, %.3f", location.latitude, location.longitude),
            countryCode = nearest?.first?.countryCode ?: "",
            latitude = location.latitude,
            longitude = location.longitude,
            timeZone = near?.timeZone ?: ZoneId.systemDefault().id,
            population = 0,
        )
    }
}

interface LocationProvider {
    fun hasPermission(): Boolean
    suspend fun currentLocation(): Location?
    fun toCity(location: Location): City
}
