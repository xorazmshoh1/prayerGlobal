package uz.xumoyun.namoz.ui.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.NotificationsOff
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import uz.xumoyun.core.common.DurationFormat
import uz.xumoyun.core.ui.components.EmptyState
import uz.xumoyun.core.ui.theme.TabularNumbers
import uz.xumoyun.namoz.R
import uz.xumoyun.namoz.domain.AsrMadhab
import uz.xumoyun.namoz.domain.CalcInput
import uz.xumoyun.namoz.domain.CalcMethod
import uz.xumoyun.namoz.domain.DayPhase
import uz.xumoyun.namoz.domain.Prayer
import uz.xumoyun.namoz.domain.PrayerCalculator
import uz.xumoyun.namoz.domain.Schedule
import uz.xumoyun.namoz.ui.NamozPreview
import uz.xumoyun.namoz.ui.TimeFormat
import uz.xumoyun.namoz.ui.components.GeometricPattern
import uz.xumoyun.namoz.ui.components.phaseContentColor
import uz.xumoyun.namoz.ui.components.phaseGradient
import uz.xumoyun.namoz.ui.prayerIcon
import uz.xumoyun.namoz.ui.prayerName
import uz.xumoyun.namoz.ui.untilPrayer
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

private val Gold = Color(0xFFC9A227)

@Composable
fun HomeRoute(
    onPickLocation: () -> Unit,
    onCalendar: () -> Unit = {},
    onSettings: () -> Unit = {},
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val travel by viewModel.travelSuggestion.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) { viewModel.checkTravel() }

    when (val s = state) {
        HomeState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        HomeState.NoLocation -> EmptyState(
            icon = Icons.Rounded.LocationOn,
            title = stringResource(R.string.no_location_title),
            message = stringResource(R.string.no_location_message),
            actionLabel = stringResource(R.string.choose_location),
            onAction = onPickLocation,
        )
        is HomeState.Ready -> HomeContent(s, onToggleAlert = viewModel::toggleAlert, onCityClick = onPickLocation, onCalendar = onCalendar, onSettings = onSettings)
    }

    travel?.let { city ->
        AlertDialog(
            onDismissRequest = viewModel::dismissTravel,
            title = { Text(stringResource(R.string.travel_title)) },
            text = { Text(stringResource(R.string.travel_message, city.name)) },
            confirmButton = { TextButton(onClick = viewModel::acceptTravel) { Text(stringResource(R.string.yes)) } },
            dismissButton = { TextButton(onClick = viewModel::dismissTravel) { Text(stringResource(R.string.no)) } },
        )
    }
}

@Composable
fun HomeContent(
    state: HomeState.Ready,
    onToggleAlert: (Prayer) -> Unit,
    onCityClick: () -> Unit,
    onCalendar: () -> Unit = {},
    onSettings: () -> Unit = {},
) {
    val context = LocalContext.current
    val locale = TimeFormat.currentLocale(context)
    val fmt = TimeFormat.formatter(context, locale)
    val onBg = phaseContentColor(state.phase)
    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
            Box(
                Modifier.fillMaxWidth().background(phaseGradient(state.phase)),
            ) {
                GeometricPattern(Modifier.matchParentSize(), color = onBg, alpha = 0.08f)
                Column(
                    Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 20.dp, vertical = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    TextButton(onClick = onCityClick) {
                        Icon(Icons.Rounded.LocationOn, null, tint = onBg)
                        Spacer(Modifier.width(4.dp))
                        Text(state.city, color = onBg, style = MaterialTheme.typography.titleMedium)
                    }
                    Text(
                        state.today.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL).withLocale(locale)),
                        color = onBg.copy(alpha = 0.9f),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Text(state.hijri, color = onBg.copy(alpha = 0.8f), style = MaterialTheme.typography.bodySmall)
                    Spacer(Modifier.height(24.dp))
                    Text(stringResource(untilPrayer(state.next.prayer)), color = onBg, style = MaterialTheme.typography.titleMedium)
                    Text(
                        DurationFormat.hms(state.next.remaining, locale),
                        color = onBg,
                        style = MaterialTheme.typography.displayMedium.merge(TabularNumbers).copy(fontWeight = FontWeight.Bold, fontSize = 52.sp),
                    )
                    Text(fmt.format(state.next.time), color = onBg.copy(alpha = 0.85f), style = MaterialTheme.typography.bodyLarge)
                    Spacer(Modifier.height(20.dp))
                }
                // Ichki ekranlar: oylik jadval va sozlamalar (TZ 6-bo'lim, pastki navigatsiyada 4 ta tab).
                Row(Modifier.align(Alignment.TopEnd).statusBarsPadding().padding(4.dp)) {
                    IconButton(onClick = onCalendar) {
                        Icon(Icons.Rounded.CalendarMonth, stringResource(R.string.tab_calendar), tint = onBg)
                    }
                    IconButton(onClick = onSettings) {
                        Icon(Icons.Rounded.Settings, stringResource(R.string.tab_settings), tint = onBg)
                    }
                }
            }
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                state.rows.forEach { row ->
                    PrayerRowCard(row, fmt.format(row.time), onToggle = { onToggleAlert(row.prayer) })
                }
            }
        }
    }
}

@Composable
private fun PrayerRowCard(row: PrayerRow, time: String, onToggle: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (row.isCurrent) Gold.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceContainerLow,
        ),
        border = if (row.isCurrent) BorderStroke(2.dp, Gold) else null,
    ) {
        Row(
            Modifier.fillMaxWidth().heightIn(min = 56.dp).padding(start = 16.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(prayerIcon(row.prayer), null, tint = if (row.isCurrent) Gold else MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(16.dp))
            Text(
                stringResource(prayerName(row.prayer)),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = if (row.isCurrent) FontWeight.Bold else FontWeight.Normal,
                modifier = Modifier.weight(1f),
            )
            Text(time, style = MaterialTheme.typography.titleMedium.merge(TabularNumbers))
            IconButton(onClick = onToggle) {
                Icon(
                    if (row.alertOn) Icons.Rounded.Notifications else Icons.Rounded.NotificationsOff,
                    contentDescription = stringResource(R.string.toggle_notification),
                    tint = if (row.alertOn) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun HomePreview() {
    val zone = ZoneId.of("Asia/Tashkent")
    val input = CalcInput(41.2995, 69.2401, zone, CalcMethod.UZBEKISTAN, AsrMadhab.HANAFI)
    val date = LocalDate.of(2026, 11, 17)
    val today = PrayerCalculator.day(input, date)
    val now = today[Prayer.ASR].minusMinutes(84)
    NamozPreview {
        HomeContent(
            HomeState.Ready(
                city = "Tashkent",
                today = date,
                hijri = "7 Jumada al-Akhirah 1448",
                rows = today.ordered.map { (p, t) -> PrayerRow(p, t, p == Prayer.DHUHR, p.isPrayer) },
                next = Schedule.next(now, today, PrayerCalculator.day(input, date.plusDays(1))),
                phase = DayPhase.DAY,
            ),
            onToggleAlert = {}, onCityClick = {},
        )
    }
}
