package uz.xumoyun.feature.hifz

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoStories
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import uz.xumoyun.core.ui.components.EmptyState
import uz.xumoyun.core.ui.components.ProgressRing
import uz.xumoyun.feature.hifz.plan.domain.QuranLayout
import uz.xumoyun.feature.hifz.plan.domain.ReviewType
import uz.xumoyun.feature.hifz.plan.domain.StepCard
import uz.xumoyun.feature.hifz.plan.domain.TodaySummary
import uz.xumoyun.feature.hifz.ui.today.HifzTodayUiState
import uz.xumoyun.feature.hifz.ui.today.HifzTodayViewModel

/**
 * Hifz tabi (TZ 6-bo'lim, ekran 4): progress halqasi va ketma-ket kunlar, uchta karta (Sabaq, Sabqi, Manzil),
 * pastda "Darsni boshlash". Mus'haf va audio shu tabdan ochiladi.
 */
@Composable
fun HifzRoute(
    modifier: Modifier = Modifier,
    onStartLesson: (String?) -> Unit = {},
    onOpenPlan: () -> Unit = {},
    onOpenProgress: () -> Unit = {},
    onOpenMushaf: () -> Unit = {},
    onOpenAudio: () -> Unit = {},
) {
    val vm: HifzTodayViewModel = hiltViewModel()
    val state by vm.state.collectAsState()

    Column(
        modifier
            .fillMaxSize()
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        TodayBody(state, onStartLesson, onOpenPlan)
        HorizontalDivider()
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = onOpenPlan, modifier = Modifier.weight(1f)) { Text(stringResource(R.string.hifz_plan_button)) }
            OutlinedButton(onClick = onOpenProgress, modifier = Modifier.weight(1f)) { Text(stringResource(R.string.hifz_progress_button)) }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = onOpenMushaf, modifier = Modifier.weight(1f)) { Text(stringResource(R.string.mushaf_open)) }
            OutlinedButton(onClick = onOpenAudio, modifier = Modifier.weight(1f)) { Text(stringResource(R.string.audio_open)) }
        }
    }
}

@Composable
private fun TodayBody(state: HifzTodayUiState, onStartLesson: (String?) -> Unit, onOpenPlan: () -> Unit) {
    when {
        state.loading -> Box(Modifier.fillMaxWidth().heightIn(min = 200.dp), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        state.failed -> EmptyState(
            icon = Icons.Rounded.ErrorOutline,
            title = stringResource(R.string.hifz_title),
            message = stringResource(R.string.hifz_load_failed),
        )
        !state.quranAvailable -> EmptyState(
            icon = Icons.Rounded.AutoStories,
            title = stringResource(R.string.hifz_title),
            message = stringResource(R.string.hifz_quran_missing),
        )
        state.summary?.hasPlan != true -> EmptyState(
            icon = Icons.Rounded.AutoStories,
            title = stringResource(R.string.hifz_no_plan_title),
            message = stringResource(R.string.hifz_no_plan_message),
            actionLabel = stringResource(R.string.hifz_create_plan),
            onAction = onOpenPlan,
        )
        else -> {
            val s = state.summary ?: return
            Header(s)
            s.cards.forEach { card ->
                StepCardView(card, state.layout) { onStartLesson(card.type.name) }
            }
            Button(
                onClick = { onStartLesson(null) },
                enabled = s.items.isNotEmpty(),
                modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
            ) {
                Text(stringResource(R.string.hifz_start_lesson), fontWeight = FontWeight.SemiBold)
            }
            if (s.completedToday) {
                Text(
                    stringResource(R.string.hifz_lesson_completed),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                )
            }
            s.finishDate?.let { date ->
                Text(
                    stringResource(R.string.hifz_finish_expected, DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).format(date)),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

@Composable
private fun Header(s: TodaySummary) {
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        ProgressRing(progress = (s.percent / 100.0).toFloat(), size = 168.dp) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    stringResource(R.string.hifz_percent_memorized, s.percent.toInt()),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                )
                Text(
                    "${s.memorized} / ${s.total}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Icon(
                Icons.Rounded.LocalFireDepartment,
                null,
                tint = MaterialTheme.colorScheme.tertiary,
                modifier = Modifier.size(20.dp),
            )
            Text(
                stringResource(R.string.hifz_streak_days, s.streakDays),
                style = MaterialTheme.typography.titleSmall,
            )
        }
    }
}

@Composable
private fun StepCardView(card: StepCard, layout: QuranLayout?, onClick: () -> Unit) {
    val label = when (card.type) {
        ReviewType.SABAQ -> stringResource(R.string.hifz_card_sabaq)
        ReviewType.SABQI -> stringResource(R.string.hifz_card_sabqi)
        ReviewType.MANZIL -> stringResource(R.string.hifz_card_manzil)
    }
    Card(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(label, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            val range = rangeLabel(card.first, card.last, layout)
            if (range == null) {
                Text(
                    stringResource(R.string.hifz_card_none),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                Text(range, style = MaterialTheme.typography.bodyLarge)
                Text(
                    stringResource(R.string.hifz_card_count_minutes, card.count, card.minutes),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

/** "Sura 1–5" yoki "Sura 2 – Sura 3 oyati" ko'rinishida oyat oralig'i. Ma'lumot yo'q bo'lsa null. */
@Composable
private fun rangeLabel(first: Int?, last: Int?, layout: QuranLayout?): String? {
    if (first == null || last == null || layout == null) return null
    val a = layout.locate(first)
    val b = layout.locate(last)
    val na = layout.surah(a.surahId)?.nameTranslit.orEmpty()
    val nb = layout.surah(b.surahId)?.nameTranslit.orEmpty()
    return if (a.surahId == b.surahId) {
        stringResource(R.string.hifz_range_one, na, a.numberInSurah, b.numberInSurah)
    } else {
        stringResource(R.string.hifz_range_span, na, a.numberInSurah, nb, b.numberInSurah)
    }
}
