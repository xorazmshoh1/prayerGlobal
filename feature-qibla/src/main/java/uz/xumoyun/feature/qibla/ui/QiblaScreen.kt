package uz.xumoyun.feature.qibla.ui

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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.CompositionLocalProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.hilt.navigation.compose.hiltViewModel
import uz.xumoyun.core.ui.components.EmptyState
import uz.xumoyun.core.ui.haptics.vibrate
import uz.xumoyun.core.ui.theme.LocalExtraColors
import uz.xumoyun.core.ui.theme.TabularNumbers
import uz.xumoyun.feature.qibla.R
import java.text.NumberFormat
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.roundToLong
import kotlin.math.sin

@Composable
fun QiblaRoute(viewModel: QiblaViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    LaunchedEffect(viewModel) {
        viewModel.alignedEvents.collect { vibrate(context, 40L) }
    }
    QiblaContent(state)
}

@Composable
fun QiblaContent(state: QiblaUiState) {
    val dialRotation by animateFloatAsState(state.dialRotationDeg, label = "dial")
    // Kompas va strelkalar RTL tillarda ham aylantirilmaydi.
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        Column(
            Modifier.fillMaxSize().statusBarsPadding().padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(stringResource(R.string.qibla_title), style = MaterialTheme.typography.headlineSmall)
            val target = state.target
            when {
                state.loading -> Unit
                target == null -> EmptyState(
                    icon = Icons.Rounded.Explore,
                    title = stringResource(R.string.qibla_no_place),
                )
                else -> {
                    Text(target.city, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(8.dp))
                    val dial = dialSize()
                    Box(Modifier.size(dial), contentAlignment = Alignment.Center) {
                        CompassDial(
                            rotationDeg = dialRotation,
                            bearingDeg = target.bearing.toFloat(),
                            aligned = state.aligned,
                            showHeading = state.headingDeg != null,
                        )
                    }
                    Text(
                        stringResource(R.string.qibla_bearing, target.bearing.roundToInt()),
                        style = MaterialTheme.typography.headlineMedium.merge(TabularNumbers),
                    )
                    Text(
                        stringResource(R.string.qibla_distance, NumberFormat.getIntegerInstance().format(target.distanceKm.roundToLong())),
                        style = MaterialTheme.typography.titleMedium.merge(TabularNumbers),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    if (!state.hasCompass) {
                        Text(
                            stringResource(R.string.compass_unavailable),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                        )
                    } else {
                        Text(
                            stringResource(if (state.aligned) R.string.qibla_aligned else R.string.qibla_turn),
                            color = if (state.aligned) LocalExtraColors.current.accent else MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.titleMedium,
                            textAlign = TextAlign.Center,
                        )
                    }
                    if (state.needsCalibration) {
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
    }
}

/** Siferblat ekranning kengligi va balandligidan kelib chiqadi (ekranning taxminan yarmi). */
@Composable
private fun dialSize(): Dp {
    val config = LocalConfiguration.current
    return minOf(config.screenWidthDp * 0.8f, 320f).dp
}

@Composable
private fun CompassDial(rotationDeg: Float, bearingDeg: Float, aligned: Boolean, showHeading: Boolean) {
    val ring = MaterialTheme.colorScheme.outline
    val primary = MaterialTheme.colorScheme.primary
    val north = MaterialTheme.colorScheme.error
    val accent = LocalExtraColors.current.accent
    val tint = if (aligned) accent else primary
    Canvas(Modifier.fillMaxSize()) {
        val c = Offset(size.width / 2f, size.height / 2f)
        val r = size.minDimension / 2f - 12.dp.toPx()
        // Siferblat: shimol doim yuqorida (aylanish -heading). Ka'ba belgisi qibla burchagida, aylana chetida.
        rotate(if (showHeading) rotationDeg else 0f, c) {
            drawCircle(tint, r, c, style = Stroke(if (aligned) 4.dp.toPx() else 2.dp.toPx()))
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
            val qa = Math.toRadians(bearingDeg - 90.0)
            val kx = c.x + (r * cos(qa)).toFloat()
            val ky = c.y + (r * sin(qa)).toFloat()
            val s = 22.dp.toPx()
            drawRect(Color(0xFF1B1B1B), Offset(kx - s / 2, ky - s / 2), Size(s, s))
            drawRect(accent, Offset(kx - s / 2, ky - s / 4), Size(s, 3.dp.toPx()))
        }
        // Telefonning yo'nalishi: markazdan yuqoriga qaragan strelka.
        val arrow = Path().apply {
            moveTo(c.x, c.y - r + 26.dp.toPx())
            lineTo(c.x - 14.dp.toPx(), c.y)
            lineTo(c.x + 14.dp.toPx(), c.y)
            close()
        }
        drawPath(arrow, tint)
        drawCircle(tint, 8.dp.toPx(), c)
    }
}
