package uz.xumoyun.feature.prayer.ui

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.rounded.DarkMode
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import uz.xumoyun.core.ui.theme.AppTheme
import uz.xumoyun.core.ui.theme.NamozHifzPalette
import uz.xumoyun.core.ui.theme.TabularNumbers
import uz.xumoyun.feature.prayer.R
import uz.xumoyun.feature.prayer.domain.AsrMadhab
import uz.xumoyun.feature.prayer.domain.CalcInput
import uz.xumoyun.feature.prayer.domain.CalcMethod
import uz.xumoyun.feature.prayer.domain.Prayer
import uz.xumoyun.feature.prayer.domain.PrayerBoard
import uz.xumoyun.feature.prayer.domain.PrayerCalculator
import uz.xumoyun.feature.prayer.domain.PrayerRow
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/**
 * Namoz tabi. [onRamadan] — Ramazon menyusi grafini ochish (navigatsiyani ilova beradi;
 * feature-prayer feature-ramadan'ga bog'lanmaydi).
 */
@Composable
fun PrayerRoute(
    onPickLocation: () -> Unit,
    onRamadan: () -> Unit = {},
    onSettings: () -> Unit = {},
    viewModel: PrayerViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val travel by viewModel.travelSuggestion.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) { viewModel.checkTravel() }

    when (val s = state) {
        PrayerState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        PrayerState.NoLocation -> EmptyState(
            icon = Icons.Rounded.LocationOn,
            title = stringResource(R.string.no_location_title),
            message = stringResource(R.string.no_location_message),
            actionLabel = stringResource(R.string.choose_location),
            onAction = onPickLocation,
        )
        is PrayerState.Ready -> PrayerContent(
            s,
            onToggleAlert = viewModel::toggleAlert,
            onCityClick = onPickLocation,
            onRamadan = onRamadan,
            onSettings = onSettings,
        )
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

/** TZ 11-bo'lim: tepada shahar va ikki sana, o'rtada keyingi namoz kartasi, ostida 6 qator. */
@Composable
fun PrayerContent(
    state: PrayerState.Ready,
    onToggleAlert: (Prayer) -> Unit,
    onCityClick: () -> Unit,
    onRamadan: () -> Unit = {},
    onSettings: () -> Unit = {},
) {
    val context = LocalContext.current
    val locale = TimeFormat.currentLocale(context)
    val fmt = TimeFormat.formatter(context, locale)
    val next = state.board.next
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).statusBarsPadding().padding(horizontal = 16.dp),
    ) {
        Row(Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onCityClick, modifier = Modifier.weight(1f, fill = false)) {
                Icon(Icons.Rounded.LocationOn, null)
                Spacer(Modifier.width(4.dp))
                Text(state.city, style = MaterialTheme.typography.titleLarge, maxLines = 1)
            }
            Spacer(Modifier.weight(1f))
            // Ramazon menyusi va sozlamalar (TZ 1 va 6-bo'limlar).
            IconButton(onClick = onRamadan) { Icon(Icons.Rounded.DarkMode, stringResource(R.string.open_ramadan)) }
            IconButton(onClick = onSettings) { Icon(Icons.Rounded.Settings, stringResource(R.string.open_settings)) }
        }
        Text(
            state.today.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL).withLocale(locale)),
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(horizontal = 12.dp),
        )
        Text(
            state.hijri,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 12.dp),
        )
        Spacer(Modifier.height(16.dp))

        // Keyingi namoz kartasi: nom, vaqt (40 sp) va teskari sanoq.
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            ),
        ) {
            Column(
                Modifier.fillMaxWidth().padding(vertical = 20.dp, horizontal = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(stringResource(untilPrayer(next.prayer)), style = MaterialTheme.typography.titleMedium)
                Text(
                    fmt.format(next.time),
                    style = MaterialTheme.typography.displaySmall.merge(TabularNumbers)
                        .copy(fontSize = 40.sp, lineHeight = 48.sp, fontWeight = FontWeight.Bold),
                )
                Text(
                    DurationFormat.hms(next.remaining, locale),
                    style = MaterialTheme.typography.titleLarge.merge(TabularNumbers),
                )
            }
        }
        Spacer(Modifier.height(16.dp))

        Column(Modifier.padding(bottom = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            state.board.rows.forEach { row ->
                PrayerRowCard(row, fmt.format(row.time), onToggle = { onToggleAlert(row.prayer) })
            }
        }
    }
}

/** Qator: nom chapda (RTL'da o'ngda), vaqt keyin, ovoz belgisi oxirida. Joriy namoz primary rangda ajratiladi. */
@Composable
private fun PrayerRowCard(row: PrayerRow, time: String, onToggle: () -> Unit) {
    val primary = MaterialTheme.colorScheme.primary
    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (row.isCurrent) primary.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceContainerLow,
        ),
        border = if (row.isCurrent) BorderStroke(2.dp, primary) else null,
    ) {
        Row(
            Modifier.fillMaxWidth().heightIn(min = 56.dp).padding(start = 16.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(prayerIcon(row.prayer), null, tint = primary)
            Spacer(Modifier.width(16.dp))
            Text(
                stringResource(prayerName(row.prayer)),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = if (row.isCurrent) FontWeight.Bold else FontWeight.Normal,
                color = if (row.isCurrent) primary else MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f),
            )
            Text(
                time,
                style = MaterialTheme.typography.titleMedium.merge(TabularNumbers),
                fontWeight = if (row.isCurrent) FontWeight.Bold else FontWeight.Normal,
            )
            IconButton(onClick = onToggle) {
                Icon(
                    if (row.alertOn) Icons.Rounded.Notifications else Icons.Rounded.NotificationsOff,
                    contentDescription = stringResource(R.string.toggle_notification),
                    tint = if (row.alertOn) primary else MaterialTheme.colorScheme.outline,
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun PrayerPreview() {
    val zone = ZoneId.of("Asia/Tashkent")
    val input = CalcInput(41.2995, 69.2401, zone, CalcMethod.MUSLIM_WORLD_LEAGUE, AsrMadhab.HANAFI)
    val date = LocalDate.of(2026, 11, 17)
    val today = PrayerCalculator.day(input, date)
    val now = today[Prayer.ASR].minusMinutes(84)
    AppTheme(NamozHifzPalette) {
        Surface {
            PrayerContent(
                PrayerState.Ready(
                    city = "Tashkent",
                    today = date,
                    hijri = "7 Jumada al-Akhirah 1448",
                    board = PrayerBoard.build(now, today, PrayerCalculator.day(input, date.plusDays(1))) { it.isPrayer },
                ),
                onToggleAlert = {}, onCityClick = {},
            )
        }
    }
}
