package uz.xumoyun.namoz.ui.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import uz.xumoyun.core.common.TimeSource
import uz.xumoyun.core.ui.theme.TabularNumbers
import uz.xumoyun.namoz.R
import uz.xumoyun.namoz.data.HijriProvider
import uz.xumoyun.namoz.data.PrayerRepository
import uz.xumoyun.namoz.domain.AsrMadhab
import uz.xumoyun.namoz.domain.CalcInput
import uz.xumoyun.namoz.domain.CalcMethod
import uz.xumoyun.namoz.domain.DayTimes
import uz.xumoyun.namoz.domain.Prayer
import uz.xumoyun.namoz.domain.PrayerCalculator
import uz.xumoyun.namoz.ui.NamozPreview
import uz.xumoyun.namoz.ui.TimeFormat
import uz.xumoyun.namoz.ui.prayerName
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.inject.Inject

data class CalendarRow(val day: DayTimes, val isToday: Boolean, val hijriDay: Int, val isRamadan: Boolean)

data class CalendarState(val month: YearMonth, val city: String, val rows: List<CalendarRow>) {
    val hasRamadan: Boolean get() = rows.any { it.isRamadan }
}

@HiltViewModel
class CalendarViewModel @Inject constructor(
    repository: PrayerRepository,
    private val hijri: HijriProvider,
    private val time: TimeSource,
) : ViewModel() {
    private val month = MutableStateFlow(YearMonth.from(time.today()))

    val state: StateFlow<CalendarState?> = combine(repository.config, month) { config, m ->
        config ?: return@combine null
        val today = LocalDate.ofInstant(time.now(), config.zone)
        CalendarState(
            month = m,
            city = config.location.name,
            rows = PrayerCalculator.month(config.input, m).map { day ->
                val h = hijri.of(day.date, config.prefs.hijriOffset)
                CalendarRow(day, day.date == today, h.day, h.isRamadan)
            },
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    fun previous() { month.value = month.value.minusMonths(1) }
    fun next() { month.value = month.value.plusMonths(1) }
}

@Composable
fun CalendarRoute(viewModel: CalendarViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    state?.let { CalendarContent(it, viewModel::previous, viewModel::next) }
}

private val DayCol = 56.dp
private val TimeCol = 60.dp

@Composable
fun CalendarContent(state: CalendarState, onPrev: () -> Unit, onNext: () -> Unit) {
    val context = LocalContext.current
    val locale = TimeFormat.currentLocale(context)
    val fmt = TimeFormat.formatter(context, locale)
    val monthFmt = DateTimeFormatter.ofPattern("LLLL yyyy", locale)
    val ramadan = state.hasRamadan
    val scroll = rememberScrollState()
    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onPrev) { Icon(Icons.AutoMirrored.Rounded.KeyboardArrowLeft, stringResource(R.string.previous_month)) }
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(monthFmt.format(state.month), style = MaterialTheme.typography.titleLarge)
                Text(state.city, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            IconButton(onClick = onNext) { Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, stringResource(R.string.next_month)) }
        }
        Column(Modifier.horizontalScroll(scroll)) {
            Row(Modifier.background(MaterialTheme.colorScheme.primaryContainer).padding(vertical = 10.dp)) {
                HeaderCell(stringResource(R.string.calendar_day), DayCol)
                Prayer.entries.forEach { HeaderCell(stringResource(prayerName(it)), TimeCol) }
                if (ramadan) {
                    HeaderCell(stringResource(R.string.suhoor), TimeCol)
                    HeaderCell(stringResource(R.string.iftar), TimeCol)
                }
            }
            LazyColumn {
                items(state.rows, key = { it.day.date.toEpochDay() }) { row ->
                    val bg = if (row.isToday) MaterialTheme.colorScheme.tertiaryContainer else MaterialTheme.colorScheme.surface
                    Row(Modifier.background(bg).padding(vertical = 10.dp)) {
                        Cell("${row.day.date.dayOfMonth}", DayCol, bold = true)
                        Prayer.entries.forEach { Cell(fmt.format(row.day[it]), TimeCol, bold = row.isToday) }
                        if (ramadan) {
                            Cell(if (row.isRamadan) fmt.format(row.day[Prayer.FAJR]) else "—", TimeCol, bold = row.isRamadan)
                            Cell(if (row.isRamadan) fmt.format(row.day[Prayer.MAGHRIB]) else "—", TimeCol, bold = row.isRamadan)
                        }
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                }
            }
        }
    }
}

@Composable
private fun HeaderCell(text: String, width: Dp) {
    Text(
        text, Modifier.width(width).padding(horizontal = 2.dp),
        style = MaterialTheme.typography.labelMedium, textAlign = TextAlign.Center,
        color = MaterialTheme.colorScheme.onPrimaryContainer, maxLines = 1,
    )
}

@Composable
private fun Cell(text: String, width: Dp, bold: Boolean = false) {
    Text(
        text, Modifier.width(width),
        style = MaterialTheme.typography.bodyMedium.merge(TabularNumbers),
        fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal,
        textAlign = TextAlign.Center,
    )
}

@Preview(showBackground = true)
@Composable
private fun CalendarPreview() {
    val input = CalcInput(41.2995, 69.2401, ZoneId.of("Asia/Tashkent"), CalcMethod.UZBEKISTAN, AsrMadhab.HANAFI)
    val m = YearMonth.of(2027, 2)
    NamozPreview {
        CalendarContent(
            CalendarState(m, "Tashkent", PrayerCalculator.month(input, m).map { CalendarRow(it, it.date.dayOfMonth == 10, it.date.dayOfMonth, it.date.dayOfMonth > 8) }),
            {}, {},
        )
    }
}
