package uz.xumoyun.feature.calendar.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import uz.xumoyun.core.ui.theme.TabularNumbers
import uz.xumoyun.feature.calendar.R
import uz.xumoyun.feature.calendar.domain.CalendarGrid
import uz.xumoyun.feature.calendar.domain.DayCell
import uz.xumoyun.feature.calendar.domain.DateConversion
import uz.xumoyun.feature.calendar.domain.EventOccurrence
import uz.xumoyun.feature.calendar.notifications.IslamicEventNotifier
import uz.xumoyun.feature.calendar.domain.PrayerDayRow
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

/** Taqvim tabi (TZ 6-bo'lim, 3-ekran). Holat [CalendarViewModel] da. */
@Composable
fun CalendarRoute(
    onIslamicEvents: () -> Unit = {},
    onRamadan: () -> Unit = {},
    viewModel: CalendarViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    CalendarContent(
        state = state,
        onPrev = viewModel::previousMonth,
        onNext = viewModel::nextMonth,
        onToday = viewModel::thisMonth,
        onMode = viewModel::setMode,
        onHijriPrimary = viewModel::setHijriPrimary,
        onConvertGregorian = viewModel::convertGregorian,
        onConvertHijri = viewModel::convertHijri,
        onClearConversion = viewModel::clearConversion,
        onIslamicEvents = onIslamicEvents,
        onRamadan = onRamadan,
    )
}

private val CellHeight = 56.dp

@Composable
fun CalendarContent(
    state: CalendarUiState,
    onPrev: () -> Unit,
    onNext: () -> Unit,
    onToday: () -> Unit,
    onMode: (CalendarMode) -> Unit,
    onHijriPrimary: (Boolean) -> Unit,
    onConvertGregorian: (LocalDate) -> Unit,
    onConvertHijri: (Int, Int, Int) -> Unit,
    onClearConversion: () -> Unit,
    onIslamicEvents: () -> Unit,
    onRamadan: () -> Unit,
) {
    val locale = remember { Locale.getDefault() }
    val monthTitle = remember(state.month, locale) { DateTimeFormatter.ofPattern("LLLL yyyy", locale).format(state.month) }
    val hijriMonths = stringArrayResource(R.array.hijri_months)
    val hijriTitle = state.today.let { "${hijriMonths.getOrElse(it.month - 1) { "" }} ${it.year}" }

    Column(Modifier.fillMaxSize().statusBarsPadding().verticalScroll(rememberScrollState())) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onPrev) { Icon(Icons.AutoMirrored.Rounded.KeyboardArrowLeft, stringResource(R.string.calendar_previous_month)) }
            Column(Modifier.weight(1f).clickable(onClick = onToday), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(monthTitle, style = MaterialTheme.typography.titleLarge)
                Text(
                    listOfNotNull(state.timetable?.city, hijriTitle).joinToString(" · "),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            IconButton(onClick = onNext) { Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, stringResource(R.string.calendar_next_month)) }
            IconButton(onClick = onIslamicEvents) { Icon(Icons.Rounded.Notifications, stringResource(R.string.islamic_events_title)) }
            IconButton(onClick = onRamadan) { Icon(Icons.Rounded.DarkMode, stringResource(R.string.ramadan_menu_open)) }
        }

        Row(Modifier.padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = state.mode == CalendarMode.GRID,
                onClick = { onMode(CalendarMode.GRID) },
                label = { Text(stringResource(R.string.calendar_mode_grid)) },
            )
            FilterChip(
                selected = state.mode == CalendarMode.TIMETABLE,
                onClick = { onMode(CalendarMode.TIMETABLE) },
                label = { Text(stringResource(R.string.calendar_mode_timetable)) },
            )
            Spacer(Modifier.weight(1f))
            FilterChip(
                selected = state.hijriPrimary,
                onClick = { onHijriPrimary(!state.hijriPrimary) },
                label = { Text(stringResource(if (state.hijriPrimary) R.string.calendar_primary_hijri else R.string.calendar_primary_gregorian)) },
            )
        }

        if (state.mode == CalendarMode.GRID) {
            GridSection(state, locale, hijriMonths)
        } else {
            TimetableSection(state.timetable?.rows.orEmpty(), state.todayDate, locale)
        }

        ConverterSection(state.conversion, state.conversionError, onConvertGregorian, onConvertHijri, onClearConversion)
    }
}

@Composable
private fun GridSection(state: CalendarUiState, locale: Locale, hijriMonths: Array<String>) {
    val weekdays = remember(locale) {
        val order = java.time.DayOfWeek.values()
        val first = java.time.temporal.WeekFields.of(locale).firstDayOfWeek
        (0 until 7).map { order[((first.ordinal + it) % 7)].getDisplayName(TextStyle.SHORT, locale) }
    }
    Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp)) {
        weekdays.forEach { d ->
            Text(d, Modifier.weight(1f), textAlign = TextAlign.Center, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
    state.grid.weeks.forEach { week ->
        Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp)) {
            week.forEach { cell ->
                Box(Modifier.weight(1f)) {
                    if (cell != null) DayCellView(cell, state.hijriPrimary, hijriMonths)
                }
            }
        }
    }
    HorizontalDivider(Modifier.padding(top = 8.dp))
    val monthEvents = state.events
    if (monthEvents.isEmpty()) {
        Text(stringResource(R.string.calendar_no_events_this_month), Modifier.padding(16.dp), style = MaterialTheme.typography.bodyMedium)
    } else {
        Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
            monthEvents.forEach { occ -> EventRow(occ, hijriMonths, locale) }
        }
    }
}

@Composable
private fun DayCellView(cell: DayCell, hijriPrimary: Boolean, hijriMonths: Array<String>) {
    val primary = if (cell.isToday) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
    val hijriText = "${cell.hijri.day}"
    val gregText = "${cell.date.dayOfMonth}"
    val big = if (hijriPrimary) hijriText else gregText
    val small = if (hijriPrimary) gregText else hijriText
    val bigLabel = if (hijriPrimary) stringResource(R.string.calendar_cell_hijri, cell.hijri.day) else stringResource(R.string.calendar_cell_gregorian, cell.date.dayOfMonth)
    Column(
        Modifier
            .height(CellHeight)
            .padding(2.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(if (cell.isToday) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface)
            .border(
                width = if (cell.isToday) 1.5.dp else 0.dp,
                color = if (cell.isToday) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(10.dp),
            )
            .semanticsCell(bigLabel),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(big, style = MaterialTheme.typography.titleMedium.merge(TabularNumbers), fontWeight = FontWeight.SemiBold, color = primary)
        Text(small, style = MaterialTheme.typography.labelSmall.merge(TabularNumbers), color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (cell.events.isNotEmpty()) {
            Box(Modifier.size(5.dp).clip(CircleShape).background(MaterialTheme.colorScheme.tertiary))
        }
    }
}

/** Katak uchun TalkBack yorlig'i (TZ 12: asosiy sana matni bilan birga). */
private fun Modifier.semanticsCell(label: String): Modifier = this.semantics { contentDescription = label }

@Composable
private fun EventRow(occ: EventOccurrence, hijriMonths: Array<String>, locale: Locale) {
    val dateText = DateTimeFormatter.ofPattern("d MMM", locale).format(occ.date)
    val hijriText = "${occ.hijri.day} ${hijriMonths.getOrElse(occ.hijri.month - 1) { "" }}"
    Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(dateText, Modifier.width(72.dp), style = MaterialTheme.typography.bodyMedium.merge(TabularNumbers))
        Column(Modifier.weight(1f)) {
            Text(stringResource(IslamicEventNotifier.nameRes(occ.event)), style = MaterialTheme.typography.bodyLarge)
            Text(hijriText, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun TimetableSection(rows: List<PrayerDayRow>, todayDate: LocalDate, locale: Locale) {
    if (rows.isEmpty()) {
        Text(stringResource(R.string.calendar_no_location), Modifier.padding(16.dp), style = MaterialTheme.typography.bodyMedium)
        return
    }
    val scroll = rememberScrollState()
    Column(Modifier.horizontalScroll(scroll).padding(horizontal = 8.dp)) {
        Row(Modifier.background(MaterialTheme.colorScheme.primaryContainer).padding(vertical = 8.dp)) {
            Text(stringResource(R.string.calendar_day), Modifier.width(72.dp), textAlign = TextAlign.Center, style = MaterialTheme.typography.labelMedium)
            rows.first().times.forEach { t ->
                Text(stringResource(t.nameRes), Modifier.width(64.dp), textAlign = TextAlign.Center, style = MaterialTheme.typography.labelMedium, maxLines = 1)
            }
        }
        Column {
            rows.forEach { row ->
                val isToday = row.date == todayDate
                Row(
                    Modifier.background(if (isToday) MaterialTheme.colorScheme.tertiaryContainer else MaterialTheme.colorScheme.surface)
                        .padding(vertical = 8.dp),
                ) {
                    Text(
                        DateTimeFormatter.ofPattern("d MMM", locale).format(row.date),
                        Modifier.width(72.dp), textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.bodySmall.merge(TabularNumbers),
                        fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal,
                    )
                    row.times.forEach { t ->
                        Text(t.text, Modifier.width(64.dp), textAlign = TextAlign.Center, style = MaterialTheme.typography.bodySmall.merge(TabularNumbers))
                    }
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ConverterSection(
    conversion: DateConversion?,
    error: Boolean,
    onGregorian: (LocalDate) -> Unit,
    onHijri: (Int, Int, Int) -> Unit,
    onClear: () -> Unit,
) {
    var showPicker by remember { mutableStateOf(false) }
    var hy by remember { mutableStateOf("") }
    var hm by remember { mutableStateOf("") }
    var hd by remember { mutableStateOf("") }
    val locale = remember { Locale.getDefault() }

    Card(Modifier.fillMaxWidth().padding(16.dp)) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(R.string.converter_title), style = MaterialTheme.typography.titleSmall)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                OutlinedButton(onClick = { showPicker = true }) { Text(stringResource(R.string.converter_from_gregorian)) }
                TextButton(onClick = onClear) { Text(stringResource(R.string.converter_clear)) }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                OutlinedTextField(value = hd, onValueChange = { hd = it.filter(Char::isDigit).take(2) }, label = { Text(stringResource(R.string.converter_day)) }, modifier = Modifier.weight(1f), singleLine = true)
                OutlinedTextField(value = hm, onValueChange = { hm = it.filter(Char::isDigit).take(2) }, label = { Text(stringResource(R.string.converter_month)) }, modifier = Modifier.weight(1f), singleLine = true)
                OutlinedTextField(value = hy, onValueChange = { hy = it.filter(Char::isDigit).take(4) }, label = { Text(stringResource(R.string.converter_year)) }, modifier = Modifier.weight(1.4f), singleLine = true)
            }
            Button(onClick = {
                val d = hd.toIntOrNull(); val m = hm.toIntOrNull(); val y = hy.toIntOrNull()
                if (d != null && m != null && y != null) onHijri(y, m, d)
            }) { Text(stringResource(R.string.converter_from_hijri)) }

            when {
                error -> Text(stringResource(R.string.converter_invalid), color = MaterialTheme.colorScheme.error)
                conversion != null -> {
                    val g = DateTimeFormatter.ofPattern("d MMMM yyyy", locale).format(conversion.gregorian)
                    val hm2 = stringArrayResource(R.array.hijri_months).getOrElse(conversion.hijri.month - 1) { "" }
                    Text(
                        stringResource(R.string.converter_result, g, conversion.hijri.day, hm2, conversion.hijri.year),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Text(stringResource(R.string.calendar_official_check), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }

    if (showPicker) {
        val pickerState = rememberDatePickerState()
        DatePickerDialog(
            onDismissRequest = { showPicker = false },
            confirmButton = {
                TextButton(onClick = {
                    pickerState.selectedDateMillis?.let { ms ->
                        onGregorian(Instant.ofEpochMilli(ms).atZone(ZoneOffset.UTC).toLocalDate())
                    }
                    showPicker = false
                }) { Text(stringResource(R.string.converter_ok)) }
            },
            dismissButton = { TextButton(onClick = { showPicker = false }) { Text(stringResource(R.string.converter_cancel)) } },
        ) {
            DatePicker(state = pickerState)
        }
    }
}
