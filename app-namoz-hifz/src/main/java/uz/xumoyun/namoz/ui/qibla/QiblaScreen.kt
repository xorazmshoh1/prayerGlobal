package uz.xumoyun.namoz.ui.qibla

import android.content.Context
import android.hardware.GeomagneticField
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Explore
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import uz.xumoyun.core.ui.components.EmptyState
import uz.xumoyun.core.ui.theme.TabularNumbers
import uz.xumoyun.namoz.R
import uz.xumoyun.feature.prayer.data.PrayerRepository
import uz.xumoyun.feature.prayer.domain.PrayerCalculator
import uz.xumoyun.namoz.ui.NamozPreview
import javax.inject.Inject
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

data class QiblaTarget(val city: String, val latitude: Double, val longitude: Double, val bearing: Double)

@HiltViewModel
class QiblaViewModel @Inject constructor(repository: PrayerRepository) : ViewModel() {
    val target: StateFlow<QiblaTarget?> = repository.config.map { c ->
        c?.let { QiblaTarget(it.location.name, it.location.latitude, it.location.longitude, PrayerCalculator.qibla(it.location.latitude, it.location.longitude)) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)
}

private val Gold = Color(0xFFC9A227)

@Composable
fun QiblaRoute(viewModel: QiblaViewModel = hiltViewModel()) {
    val target by viewModel.target.collectAsStateWithLifecycle()
    val t = target ?: return
    val context = LocalContext.current
    val sensors = remember { context.getSystemService(Context.SENSOR_SERVICE) as SensorManager }
    val rotationSensor = remember { sensors.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR) }
    val heading = remember { mutableFloatStateOf(0f) }
    val accuracy = remember { mutableIntStateOf(SensorManager.SENSOR_STATUS_UNRELIABLE) }

    if (rotationSensor == null) {
        EmptyState(
            icon = Icons.Rounded.Explore,
            title = stringResource(R.string.compass_unavailable),
            message = stringResource(R.string.qibla_bearing, t.bearing.roundToInt()),
        )
        return
    }

    DisposableEffect(rotationSensor) {
        // Magnit shimoldan haqiqiy shimolga o'tkazish uchun og'ish burchagi.
        val declination = GeomagneticField(t.latitude.toFloat(), t.longitude.toFloat(), 0f, System.currentTimeMillis()).declination
        val matrix = FloatArray(9)
        val orientation = FloatArray(3)
        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                SensorManager.getRotationMatrixFromVector(matrix, event.values)
                SensorManager.getOrientation(matrix, orientation)
                val azimuth = Math.toDegrees(orientation[0].toDouble()).toFloat() + declination
                heading.floatValue = (azimuth + 360f) % 360f
            }
            override fun onAccuracyChanged(sensor: Sensor, value: Int) { accuracy.intValue = value }
        }
        sensors.registerListener(listener, rotationSensor, SensorManager.SENSOR_DELAY_UI)
        onDispose { sensors.unregisterListener(listener) }
    }

    QiblaContent(t, heading.floatValue, accuracy.intValue)
}

@Composable
fun QiblaContent(target: QiblaTarget, heading: Float, accuracy: Int) {
    // Kompas va strelkalar RTL tillarda ham aylantirilmaydi.
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        val diff = ((target.bearing - heading + 540) % 360 - 180).toFloat()
        val aligned = abs(diff) < 4f
        val dialRotation by animateFloatAsState(-heading, label = "dial")
        Column(
            Modifier.fillMaxSize().statusBarsPadding().padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(stringResource(R.string.qibla_title), style = MaterialTheme.typography.headlineSmall)
            Text(target.city, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(8.dp))
            Box(Modifier.size(300.dp), contentAlignment = Alignment.Center) {
                Compass(dialRotation, target.bearing.toFloat(), aligned)
            }
            Text(
                stringResource(R.string.qibla_bearing, target.bearing.roundToInt()),
                style = MaterialTheme.typography.headlineMedium.merge(TabularNumbers),
            )
            Text(
                stringResource(if (aligned) R.string.qibla_aligned else R.string.qibla_turn),
                color = if (aligned) Gold else MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.titleMedium,
            )
            if (accuracy <= SensorManager.SENSOR_STATUS_ACCURACY_LOW) {
                Card(Modifier.padding(top = 8.dp)) {
                    Text(
                        stringResource(R.string.calibrate_hint),
                        Modifier.padding(16.dp),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        }
    }
}

@Composable
private fun Compass(dialRotation: Float, bearing: Float, aligned: Boolean) {
    val ring = MaterialTheme.colorScheme.outline
    val primary = MaterialTheme.colorScheme.primary
    val north = MaterialTheme.colorScheme.error
    Canvas(Modifier.fillMaxSize()) {
        val c = Offset(size.width / 2, size.height / 2)
        val r = size.minDimension / 2 - 8.dp.toPx()
        rotate(dialRotation, c) {
            drawCircle(ring, r, c, style = Stroke(2.dp.toPx()))
            for (i in 0 until 72) {
                val a = Math.toRadians(i * 5.0 - 90)
                val len = if (i % 18 == 0) 18.dp.toPx() else if (i % 2 == 0) 10.dp.toPx() else 5.dp.toPx()
                drawLine(
                    if (i == 0) north else ring,
                    Offset(c.x + (r * cos(a)).toFloat(), c.y + (r * sin(a)).toFloat()),
                    Offset(c.x + ((r - len) * cos(a)).toFloat(), c.y + ((r - len) * sin(a)).toFloat()),
                    strokeWidth = if (i % 18 == 0) 3.dp.toPx() else 1.5.dp.toPx(),
                )
            }
            // Ka'ba belgisi: qibla yo'nalishida kichik kvadrat.
            val qa = Math.toRadians(bearing - 90.0)
            val kx = c.x + ((r - 40.dp.toPx()) * cos(qa)).toFloat()
            val ky = c.y + ((r - 40.dp.toPx()) * sin(qa)).toFloat()
            val s = 22.dp.toPx()
            rotate(bearing, Offset(kx, ky)) {
                drawRect(Color(0xFF1B1B1B), Offset(kx - s / 2, ky - s / 2), androidx.compose.ui.geometry.Size(s, s))
                drawRect(Gold, Offset(kx - s / 2, ky - s / 4), androidx.compose.ui.geometry.Size(s, 3.dp.toPx()))
            }
            drawLine(Gold.copy(alpha = 0.6f), c, Offset(kx, ky), strokeWidth = 3.dp.toPx())
        }
        // Telefonning yo'nalishi: tepaga qaragan strelka.
        val arrow = Path().apply {
            moveTo(c.x, c.y - r + 26.dp.toPx())
            lineTo(c.x - 14.dp.toPx(), c.y)
            lineTo(c.x + 14.dp.toPx(), c.y)
            close()
        }
        drawPath(arrow, if (aligned) Gold else primary)
        drawCircle(if (aligned) Gold else primary, 8.dp.toPx(), c)
    }
}

@Preview(showBackground = true)
@Composable
private fun QiblaPreview() {
    NamozPreview { QiblaContent(QiblaTarget("Tashkent", 41.3, 69.24, 241.0), heading = 230f, accuracy = SensorManager.SENSOR_STATUS_ACCURACY_LOW) }
}
