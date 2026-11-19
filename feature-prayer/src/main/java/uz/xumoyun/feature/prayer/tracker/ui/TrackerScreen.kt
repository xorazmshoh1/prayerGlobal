package uz.xumoyun.feature.prayer.tracker.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import uz.xumoyun.core.ui.theme.TabularNumbers
import uz.xumoyun.feature.prayer.R
import uz.xumoyun.feature.prayer.domain.Prayer
import uz.xumoyun.feature.prayer.tracker.domain.PrayerLog
import uz.xumoyun.feature.prayer.tracker.domain.PrayerStatus
import uz.xumoyun.feature.prayer.tracker.domain.Qada
import uz.xumoyun.feature.prayer.tracker.domain.QadaPrayer
import uz.xumoyun.feature.prayer.ui.prayerName
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

/** TZ 6-bo'lim, ekran 11: namoz kuzatuvchisi (hafta) va qazo hisoblagich. */
@Composable
fun TrackerRoute(viewModel: TrackerViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var wizardOpen by rememberSaveable { mutableStateOf(false) }
    var editing by remember { mutableStateOf<QadaPrayer?>(null) }

    Column(
        Modifier.fillMaxWidth().statusBarsPadding().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        StreakCard(state.streak)
        WeekCard(
            state = state,
            onPrev = { viewModel.shiftWeek(-1) },
            onNext = { viewModel.shiftWeek(1) },
            onCell = { date, prayer -> viewModel.cycleStatus(date, prayer) },
        )
        QadaCard(
            state = state,
            onAdjust = { p, d -> viewModel.adjust(p, d) },
            onEdit = { editing = it },
            onGoal = { viewModel.setDailyGoal(it) },
            onUndo = { viewModel.undo() },
            onWizard = { wizardOpen = true },
        )
    }

    if (wizardOpen) {
        EstimateWizard(
            today = state.today,
            onDismiss = { wizardOpen = false },
            onApply = { start, end -> viewModel.applyEstimate(start, end) },
        )
    }
    editing?.let { prayer ->
        val current = state.counters.firstOrNull { it.prayer == prayer }?.remaining ?: 0
        SetRemainingDialog(
            current = current,
            onDismiss = { editing = null },
            onSave = { viewModel.setRemaining(prayer, it) },
        )
    }
}

@Composable
private fun StreakCard(streak: Int) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(stringResource(R.string.tracker_streak, streak), style = MaterialTheme.typography.titleMedium)
            Text(stringResource(R.string.tracker_streak_hint), style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun WeekCard(
    state: TrackerUiState,
    onPrev: () -> Unit,
    onNext: () -> Unit,
    onCell: (LocalDate, Prayer) -> Unit,
) {
    val dayFormat = remember { DateTimeFormatter.ofPattern("EEE d", Locale.getDefault()) }
    val rangeFormat = remember { DateTimeFormatter.ofPattern("d MMM", Locale.getDefault()) }
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onPrev) {
                    Icon(Icons.AutoMirrored.Rounded.KeyboardArrowLeft, stringResource(R.string.tracker_week_prev))
                }
                Text(
                    "${state.week.first().format(rangeFormat)} – ${state.week.last().format(rangeFormat)}",
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                )
                IconButton(onClick = onNext) {
                    Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, stringResource(R.string.tracker_week_next))
                }
            }
            Row(Modifier.fillMaxWidth()) {
                Spacer(Modifier.width(84.dp))
                PrayerLog.TRACKED.forEach { p ->
                    Text(
                        stringResource(prayerName(p)),
                        style = MaterialTheme.typography.labelSmall,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
            state.week.forEach { date ->
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        date.format(dayFormat),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = if (date == state.today) FontWeight.Bold else FontWeight.Normal,
                        modifier = Modifier.width(84.dp),
                    )
                    PrayerLog.TRACKED.forEach { p ->
                        val status = state.logs[date]?.get(p)
                        StatusCell(
                            status = status,
                            modifier = Modifier.weight(1f),
                            onClick = { onCell(date, p) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StatusCell(status: PrayerStatus?, modifier: Modifier, onClick: () -> Unit) {
    val label = when (status) {
        PrayerStatus.PRAYED -> stringResource(R.string.tracker_state_prayed)
        PrayerStatus.MISSED -> stringResource(R.string.tracker_state_missed)
        PrayerStatus.QADA -> stringResource(R.string.tracker_state_missed)
        null -> stringResource(R.string.tracker_state_none)
    }
    Box(modifier.padding(2.dp), contentAlignment = Alignment.Center) {
        Box(
            Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(
                    when (status) {
                        PrayerStatus.PRAYED -> MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
                        PrayerStatus.MISSED -> MaterialTheme.colorScheme.error.copy(alpha = 0.14f)
                        else -> Color.Transparent
                    },
                )
                .clickable(onClick = onClick)
                .semantics { stateDescription = label },
            contentAlignment = Alignment.Center,
        ) {
            when (status) {
                PrayerStatus.PRAYED -> Icon(Icons.Rounded.Check, null, tint = MaterialTheme.colorScheme.primary)
                PrayerStatus.MISSED, PrayerStatus.QADA -> Icon(Icons.Rounded.Close, null, tint = MaterialTheme.colorScheme.error)
                null -> Box(Modifier.size(10.dp).clip(CircleShape).background(MaterialTheme.colorScheme.outlineVariant))
            }
        }
    }
}

@Composable
private fun QadaCard(
    state: TrackerUiState,
    onAdjust: (QadaPrayer, Int) -> Unit,
    onEdit: (QadaPrayer) -> Unit,
    onGoal: (Int) -> Unit,
    onUndo: () -> Unit,
    onWizard: () -> Unit,
) {
    val finish = state.finishDate
    val finishText = when {
        state.totalRemaining == 0 -> stringResource(R.string.tracker_qada_done)
        finish == null -> stringResource(R.string.tracker_qada_no_goal)
        else -> stringResource(
            R.string.tracker_qada_finish,
            finish.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(Locale.getDefault())),
        )
    }
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(stringResource(R.string.tracker_qada_title), style = MaterialTheme.typography.titleMedium)
            Text(
                stringResource(R.string.tracker_qada_total, state.totalRemaining),
                style = MaterialTheme.typography.headlineSmall.merge(TabularNumbers),
            )
            Text(finishText, style = MaterialTheme.typography.bodyMedium)

            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.tracker_daily_goal), modifier = Modifier.weight(1f))
                StepButton("−") { onGoal(state.dailyGoal - 1) }
                Text(state.dailyGoal.toString(), modifier = Modifier.width(48.dp), textAlign = TextAlign.Center, style = TabularNumbers)
                StepButton("+") { onGoal(state.dailyGoal + 1) }
            }

            state.visibleCounters.forEach { c ->
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(qadaName(c.prayer)), modifier = Modifier.weight(1f))
                    StepButton("−") { onAdjust(c.prayer, -1) }
                    TextButton(onClick = { onEdit(c.prayer) }, modifier = Modifier.width(72.dp)) {
                        Text(c.remaining.toString(), style = TabularNumbers, textAlign = TextAlign.Center)
                    }
                    StepButton("+") { onAdjust(c.prayer, +1) }
                }
            }

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onUndo, enabled = state.canUndo, modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.tracker_undo))
                }
                Button(onClick = onWizard, modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.tracker_wizard_button))
                }
            }
            Text(stringResource(R.string.tracker_qada_hint), style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun StepButton(label: String, onClick: () -> Unit) {
    OutlinedButton(onClick = onClick, modifier = Modifier.width(52.dp)) { Text(label) }
}

private fun qadaName(p: QadaPrayer): Int = when (p) {
    QadaPrayer.FAJR -> prayerName(Prayer.FAJR)
    QadaPrayer.DHUHR -> prayerName(Prayer.DHUHR)
    QadaPrayer.ASR -> prayerName(Prayer.ASR)
    QadaPrayer.MAGHRIB -> prayerName(Prayer.MAGHRIB)
    QadaPrayer.ISHA -> prayerName(Prayer.ISHA)
    QadaPrayer.WITR -> R.string.tracker_prayer_witr
    QadaPrayer.FAST -> R.string.tracker_prayer_fast
}

/** Taxminiy hisoblash ustasi: boshlanish va tugash sanasi; natija — kunlar × namoz soni. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EstimateWizard(today: LocalDate, onDismiss: () -> Unit, onApply: (LocalDate, LocalDate) -> Unit) {
    var start by rememberSaveable { mutableStateOf(today.minusYears(1).toString()) }
    var end by rememberSaveable { mutableStateOf(today.toString()) }
    var picking by rememberSaveable { mutableStateOf<Int?>(null) }
    val s = LocalDate.parse(start)
    val e = LocalDate.parse(end)
    val days = Qada.inclusiveDays(s, e)
    val format = remember { DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(Locale.getDefault()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.tracker_wizard_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { picking = 0 }, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.tracker_wizard_start) + ": " + s.format(format))
                }
                OutlinedButton(onClick = { picking = 1 }, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.tracker_wizard_end) + ": " + e.format(format))
                }
                Text(
                    if (days > 0) stringResource(R.string.tracker_wizard_result, days, 5, days * 5)
                    else stringResource(R.string.tracker_wizard_invalid),
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text(stringResource(R.string.tracker_wizard_disclaimer), style = MaterialTheme.typography.bodySmall)
            }
        },
        confirmButton = {
            TextButton(enabled = days > 0, onClick = { onApply(s, e); onDismiss() }) {
                Text(stringResource(R.string.tracker_wizard_apply))
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.tracker_cancel)) } },
    )

    picking?.let { which ->
        val initial = (if (which == 0) s else e).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        val pickerState = rememberDatePickerState(initialSelectedDateMillis = initial)
        DatePickerDialog(
            onDismissRequest = { picking = null },
            confirmButton = {
                TextButton(onClick = {
                    pickerState.selectedDateMillis?.let { millis ->
                        val picked = Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate().toString()
                        if (which == 0) start = picked else end = picked
                    }
                    picking = null
                }) { Text(stringResource(R.string.tracker_ok)) }
            },
            dismissButton = { TextButton(onClick = { picking = null }) { Text(stringResource(R.string.tracker_cancel)) } },
        ) { DatePicker(state = pickerState) }
    }
}

@Composable
private fun SetRemainingDialog(current: Int, onDismiss: () -> Unit, onSave: (Int) -> Unit) {
    var text by rememberSaveable { mutableStateOf(current.toString()) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.tracker_set_title)) },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it.filter(Char::isDigit).take(7) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            )
        },
        confirmButton = {
            TextButton(onClick = {
                text.toIntOrNull()?.let(onSave)
                onDismiss()
            }) { Text(stringResource(R.string.tracker_save)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.tracker_cancel)) } },
    )
}
