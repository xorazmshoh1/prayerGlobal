package uz.xumoyun.feature.hifz.ui.progress

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import java.time.format.TextStyle
import java.util.Locale
import uz.xumoyun.core.ui.components.SectionTitle
import uz.xumoyun.feature.hifz.R
import uz.xumoyun.feature.hifz.plan.domain.DayReview
import uz.xumoyun.feature.hifz.plan.domain.JuzCell
import uz.xumoyun.feature.hifz.plan.domain.JuzStatus
import uz.xumoyun.feature.hifz.plan.domain.SurahProgress
import uz.xumoyun.feature.hifz.plan.domain.SurahStatus

/** Progress ekrani (TZ 6-bo'lim, ekran 7): 30 katakli juz xaritasi, sura holati, haftalik grafik. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProgressRoute(onBack: () -> Unit) {
    val vm: ProgressViewModel = hiltViewModel()
    val state by vm.state.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.progress_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, stringResource(R.string.audio_back))
                    }
                },
            )
        },
    ) { padding ->
        val body = Modifier.fillMaxSize().padding(padding)
        when {
            state.loading -> Box(body) {}
            state.failed || !state.quranAvailable -> Box(body.padding(16.dp)) {
                Text(stringResource(if (state.failed) R.string.hifz_load_failed else R.string.hifz_quran_missing))
            }
            else -> LazyColumn(
                body,
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                item {
                    Text(
                        stringResource(R.string.progress_summary, state.memorized, state.total),
                        style = MaterialTheme.typography.titleMedium,
                    )
                    LinearProgressIndicator(
                        progress = (state.percent / 100.0).toFloat(),
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    )
                }
                item { SectionTitle(stringResource(R.string.progress_juz_map)) }
                items(state.juz.chunked(JUZ_COLUMNS)) { row -> JuzRow(row) }
                item { SectionTitle(stringResource(R.string.progress_last_week)) }
                item { WeeklyChart(state.weekly) }
                item { SectionTitle(stringResource(R.string.progress_surahs)) }
                items(state.surahs, key = { it.surah.id }) { row -> SurahRow(row) }
            }
        }
    }
}

private const val JUZ_COLUMNS = 6

@Composable
private fun JuzRow(row: List<JuzCell>) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        row.forEach { cell ->
            val (bg, fg) = when (cell.status) {
                JuzStatus.DONE -> MaterialTheme.colorScheme.primary to MaterialTheme.colorScheme.onPrimary
                JuzStatus.PARTIAL -> MaterialTheme.colorScheme.primaryContainer to MaterialTheme.colorScheme.onPrimaryContainer
                JuzStatus.NONE -> MaterialTheme.colorScheme.surfaceVariant to MaterialTheme.colorScheme.onSurfaceVariant
            }
            Box(
                Modifier
                    .weight(1f)
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(bg),
                contentAlignment = Alignment.Center,
            ) {
                Text(cell.juz.toString(), color = fg, fontWeight = FontWeight.SemiBold)
            }
        }
        repeat(JUZ_COLUMNS - row.size) { Box(Modifier.weight(1f)) {} }
    }
}

@Composable
private fun WeeklyChart(days: List<DayReview>) {
    val max = (days.maxOfOrNull { it.ayahs } ?: 0).coerceAtLeast(1)
    val barColor = MaterialTheme.colorScheme.primary
    val track = MaterialTheme.colorScheme.surfaceVariant
    Row(Modifier.fillMaxWidth().height(140.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
        days.forEach { day ->
            Column(Modifier.weight(1f).fillMaxSize().padding(horizontal = 4.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    day.ayahs.toString(),
                    style = MaterialTheme.typography.labelSmall,
                    maxLines = 1,
                )
                Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.BottomCenter) {
                    Canvas(Modifier.width(14.dp).fillMaxSize()) {
                        drawRoundRect(color = track, cornerRadius = CornerRadius(7f, 7f))
                        val fraction = day.ayahs.toFloat() / max
                        if (day.ayahs > 0) {
                            val h = size.height * fraction.coerceAtLeast(0.06f)
                            drawRoundRect(
                                color = barColor,
                                topLeft = Offset(0f, size.height - h),
                                size = Size(size.width, h),
                                cornerRadius = CornerRadius(7f, 7f),
                            )
                        }
                    }
                }
                Text(
                    day.date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault()),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun SurahRow(row: SurahProgress) {
    val statusText = stringResource(
        when (row.status) {
            SurahStatus.MEMORIZED -> R.string.surah_status_memorized
            SurahStatus.IN_PROGRESS -> R.string.surah_status_in_progress
            SurahStatus.NOT_STARTED -> R.string.surah_status_not_started
        },
    )
    Row(
        Modifier.fillMaxWidth().padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(row.surah.id.toString(), style = MaterialTheme.typography.labelLarge, modifier = Modifier.width(36.dp), textAlign = TextAlign.Center)
        Column(Modifier.weight(1f)) {
            Text(row.surah.nameTranslit, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                "${row.memorized} / ${row.total} · $statusText",
                style = MaterialTheme.typography.bodySmall,
                color = if (row.status == SurahStatus.MEMORIZED) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
