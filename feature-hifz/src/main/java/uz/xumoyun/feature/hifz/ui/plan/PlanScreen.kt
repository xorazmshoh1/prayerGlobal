package uz.xumoyun.feature.hifz.ui.plan

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import uz.xumoyun.feature.hifz.R
import uz.xumoyun.feature.hifz.plan.domain.HifzReminder
import uz.xumoyun.feature.hifz.plan.domain.PlanTarget
import uz.xumoyun.feature.hifz.plan.domain.QURAN_AYAH_COUNT

/** Hifz rejasi (TZ 6-bo'lim, ekran 6): maqsad, kunlik oyatlar, eslatma va tugash sanasi prognozi. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlanRoute(onBack: () -> Unit) {
    val vm: PlanViewModel = hiltViewModel()
    val state by vm.state.collectAsState()

    LaunchedEffect(state.saved) { if (state.saved) onBack() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.plan_title)) },
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
            state.loading -> Column(body) {}
            state.failed || !state.quranAvailable -> Column(body.padding(16.dp)) {
                Text(stringResource(if (state.failed) R.string.hifz_load_failed else R.string.hifz_quran_missing))
            }
            else -> Column(
                body
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                GoalSection(state, vm)
                DailySection(state.dailyNewAyahs) { vm.changeDaily(it) }
                ReminderSection(state, vm)
                state.preview?.let { p ->
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                stringResource(R.string.plan_preview_total, p.total, p.remaining),
                                style = MaterialTheme.typography.bodyMedium,
                            )
                            Text(
                                stringResource(
                                    R.string.hifz_finish_expected,
                                    DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).format(p.finishDate),
                                ),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
                Button(onClick = { vm.save() }, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)) {
                    Text(stringResource(R.string.plan_save), fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
private fun GoalSection(state: PlanUiState, vm: PlanViewModel) {
    Text(stringResource(R.string.plan_goal), style = MaterialTheme.typography.titleMedium)
    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        FilterChip(
            selected = state.target is PlanTarget.All,
            onClick = { vm.selectTarget(PlanTarget.All) },
            label = { Text(stringResource(R.string.plan_goal_all)) },
        )
        FilterChip(
            selected = state.target is PlanTarget.Juz,
            onClick = { vm.selectTarget(PlanTarget.Juz(1)) },
            label = { Text(stringResource(R.string.plan_goal_juz)) },
        )
        FilterChip(
            selected = state.target is PlanTarget.Surah,
            onClick = { vm.selectTarget(PlanTarget.Surah(1)) },
            label = { Text(stringResource(R.string.plan_goal_surah)) },
        )
    }
    when (val t = state.target) {
        is PlanTarget.Juz -> Stepper(
            label = stringResource(R.string.plan_selected_juz, t.juz),
            onLess = { vm.selectTarget(PlanTarget.Juz((t.juz - 1).coerceAtLeast(1))) },
            onMore = { vm.selectTarget(PlanTarget.Juz((t.juz + 1).coerceAtMost(30))) },
        )
        is PlanTarget.Surah -> {
            val s = state.surahs.firstOrNull { it.id == t.surahId }
            Stepper(
                label = stringResource(R.string.plan_selected_surah, t.surahId, s?.nameTranslit.orEmpty(), s?.ayahCount ?: 0),
                onLess = { vm.selectTarget(PlanTarget.Surah((t.surahId - 1).coerceAtLeast(1))) },
                onMore = { vm.selectTarget(PlanTarget.Surah((t.surahId + 1).coerceAtMost(114))) },
            )
        }
        PlanTarget.All -> Text(
            stringResource(R.string.plan_all_hint, QURAN_AYAH_COUNT),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun DailySection(value: Int, onChange: (Int) -> Unit) {
    Text(stringResource(R.string.plan_daily), style = MaterialTheme.typography.titleMedium)
    Stepper(
        label = value.toString(),
        onLess = { onChange(-1) },
        onMore = { onChange(1) },
    )
    Text(
        stringResource(R.string.plan_daily_hint),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun ReminderSection(state: PlanUiState, vm: PlanViewModel) {
    Text(stringResource(R.string.plan_reminder), style = MaterialTheme.typography.titleMedium)
    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        FilterChip(
            selected = state.reminderPrayer == null,
            onClick = { vm.setReminderPrayer(null) },
            label = { Text(stringResource(R.string.plan_reminder_off)) },
        )
        HifzReminder.PRAYER_KEYS.forEach { key ->
            FilterChip(
                selected = state.reminderPrayer == key,
                onClick = { vm.setReminderPrayer(key) },
                label = { Text(stringResource(prayerLabel(key))) },
            )
        }
    }
    if (state.reminderPrayer != null) {
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            HifzReminder.OFFSETS_MIN.forEach { minutes ->
                FilterChip(
                    selected = state.reminderOffsetMin == minutes,
                    onClick = { vm.setReminderOffset(minutes) },
                    label = {
                        Text(
                            if (minutes == 0) stringResource(R.string.plan_offset_zero)
                            else stringResource(R.string.plan_offset_after, minutes),
                        )
                    },
                )
            }
        }
    }
}

private fun prayerLabel(key: String): Int = when (key) {
    "FAJR" -> R.string.prayer_fajr
    "DHUHR" -> R.string.prayer_dhuhr
    "ASR" -> R.string.prayer_asr
    "MAGHRIB" -> R.string.prayer_maghrib
    else -> R.string.prayer_isha
}

@Composable
private fun Stepper(label: String, onLess: () -> Unit, onMore: () -> Unit) {
    Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
        IconButton(onClick = onLess) { Icon(Icons.Rounded.Remove, stringResource(R.string.lesson_less)) }
        Text(label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(horizontal = 8.dp))
        IconButton(onClick = onMore) { Icon(Icons.Rounded.Add, stringResource(R.string.lesson_more)) }
    }
}
