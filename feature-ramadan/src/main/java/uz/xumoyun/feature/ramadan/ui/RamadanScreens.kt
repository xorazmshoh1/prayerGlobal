package uz.xumoyun.feature.ramadan.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import uz.xumoyun.core.common.hijri.HijriYmd
import uz.xumoyun.core.ui.components.EmptyState
import uz.xumoyun.core.ui.theme.TabularNumbers
import uz.xumoyun.feature.ramadan.R
import uz.xumoyun.feature.ramadan.domain.FastStatus
import uz.xumoyun.feature.ramadan.domain.RamadanCalc
import uz.xumoyun.feature.ramadan.domain.RamadanDayRow
import uz.xumoyun.feature.ramadan.domain.RamadanPhase
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

/** Ramazon menyusi marshrutlari (navigatsiya grafi app'da, ekranlar shu yerda). */
object RamadanRoutes {
    const val GRAPH = "ramadan"
    const val HOME = "ramadan/home"
    const val DAYS = "ramadan/days"
    const val FASTS = "ramadan/fasts"
    const val DUAS = "ramadan/duas"
    const val KHATM = "ramadan/khatm"
    const val SETTINGS = "ramadan/settings"
}

/** Vaqtni mahalliy zona bilan ko'rsatish (Ramazon ekranlari uchun). */
internal fun timeText(instant: Instant?, zone: ZoneId?, locale: Locale = Locale.getDefault()): String {
    if (instant == null) return "—"
    val z = zone ?: ZoneId.systemDefault()
    return DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).withLocale(locale).format(instant.atZone(z))
}

/** "3 h 05 min" ko'rinishidagi qolgan vaqt (manfiy bo'lsa — 0). */
internal fun countdownText(from: Instant, to: Instant?): String {
    if (to == null) return "—"
    val d = Duration.between(from, to)
    if (d.isNegative || d.isZero) return "0"
    val h = d.toHours()
    val m = d.toMinutesPart()
    return if (h > 0) "%d h %02d min".format(h, m) else "%d min".format(m)
}

@Composable
fun RamadanHomeRoute(
    onOpenDays: () -> Unit,
    onOpenDuas: () -> Unit,
    onOpenKhatm: () -> Unit,
    onOpenFasts: () -> Unit,
    onOpenSettings: () -> Unit,
    viewModel: RamadanHomeViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val s = state
    if (s == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            EmptyState(icon = Icons.Rounded.DarkMode, title = stringResource(R.string.ramadan_title), message = stringResource(R.string.ramadan_need_location))
        }
        return
    }
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        val title = when (s.phase) {
            RamadanPhase.DURING -> stringResource(R.string.ramadan_day_title, s.dayNumber ?: 0)
            RamadanPhase.BEFORE -> stringResource(R.string.ramadan_starts_in, s.daysUntilStart.toInt())
            RamadanPhase.AFTER -> stringResource(R.string.ramadan_next_year)
        }
        Text(title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
        Text(
            listOfNotNull(s.city, hijriLabel(s.hijri)).joinToString(" · "),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (s.phase == RamadanPhase.DURING) {
            val done = (s.dayNumber ?: 0)
            LinearProgressIndicator(progress = { done / RamadanCalc.DAYS.toFloat() }, Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                TimeCard(
                    label = stringResource(R.string.suhoor),
                    time = timeText(s.suhoorAt, s.zone),
                    remaining = countdownText(s.now, s.suhoorAt?.takeIf { it.isAfter(s.now) }),
                    modifier = Modifier.weight(1f),
                )
                TimeCard(
                    label = stringResource(R.string.iftar),
                    time = timeText(s.iftarAt, s.zone),
                    remaining = countdownText(s.now, s.iftarAt?.takeIf { it.isAfter(s.now) }),
                    modifier = Modifier.weight(1f),
                )
            }
        }
        Text(stringResource(R.string.ramadan_quick_links), style = MaterialTheme.typography.titleSmall)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            QuickLink(stringResource(R.string.ramadan_calendar_title), onOpenDays, Modifier.weight(1f))
            QuickLink(stringResource(R.string.ramadan_duas_title), onOpenDuas, Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            QuickLink(stringResource(R.string.ramadan_khatm_title), onOpenKhatm, Modifier.weight(1f))
            QuickLink(stringResource(R.string.ramadan_fasts_title), onOpenFasts, Modifier.weight(1f))
        }
        OutlinedButton(onClick = onOpenSettings, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.ramadan_settings_title)) }
    }
}

private fun hijriLabel(h: HijriYmd): String = "${h.day}/${h.month}/${h.year} AH"

@Composable
private fun TimeCard(label: String, time: String, remaining: String, modifier: Modifier = Modifier) {
    Card(modifier, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
        Column(Modifier.padding(14.dp)) {
            Text(label, style = MaterialTheme.typography.labelLarge)
            Text(time, style = MaterialTheme.typography.headlineMedium.merge(TabularNumbers), fontWeight = FontWeight.Bold)
            Text(remaining, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun QuickLink(label: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Card(modifier.clickable(onClick = onClick)) {
        Text(label, Modifier.padding(14.dp), style = MaterialTheme.typography.bodyLarge)
    }
}

/** R2 — 30 qatorli Ramazon taqvimi, bugungi qator ajratilgan; "Ulashish" tugmasi. */
@Composable
fun RamadanDaysRoute(onShare: (RamadanDaysState) -> Unit, viewModel: RamadanDaysViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val s = state ?: return
    val locale = remember { Locale.getDefault() }
    val scroll = rememberScrollState()
    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.ramadan_calendar_title), Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
            OutlinedButton(onClick = { onShare(s) }) { Text(stringResource(R.string.ramadan_share)) }
        }
        Column(Modifier.horizontalScroll(scroll).padding(horizontal = 8.dp).verticalScroll(rememberScrollState())) {
            Row(Modifier.background(MaterialTheme.colorScheme.primaryContainer).padding(vertical = 8.dp)) {
                HeaderText(stringResource(R.string.ramadan_col_day), 44)
                HeaderText(stringResource(R.string.ramadan_col_date), 84)
                HeaderText(stringResource(R.string.ramadan_col_hijri), 84)
                HeaderText(stringResource(R.string.suhoor), 64)
                HeaderText(stringResource(R.string.iftar), 64)
            }
            s.rows.forEach { row -> DayRowView(row, locale, s) }
        }
    }
}

@Composable
private fun HeaderText(text: String, width: Int) {
    Text(text, Modifier.width(width.dp), style = MaterialTheme.typography.labelMedium, textAlign = TextAlign.Center, maxLines = 1)
}

@Composable
private fun DayRowView(row: RamadanDayRow, locale: Locale, s: RamadanDaysState) {
    val bg = if (row.isToday) MaterialTheme.colorScheme.tertiaryContainer else MaterialTheme.colorScheme.surface
    val zone = ZoneId.systemDefault()
    val dateText = DateTimeFormatter.ofPattern("d MMM", locale).format(row.date)
    Row(Modifier.background(bg).padding(vertical = 8.dp)) {
        Text("${row.number}", Modifier.width(44.dp), textAlign = TextAlign.Center, fontWeight = FontWeight.Bold)
        Text(dateText, Modifier.width(84.dp), textAlign = TextAlign.Center, style = MaterialTheme.typography.bodySmall)
        Text("${row.hijri.day}/${row.hijri.month}", Modifier.width(84.dp), textAlign = TextAlign.Center, style = MaterialTheme.typography.bodySmall.merge(TabularNumbers))
        Text(timeText(row.imsak, zone, locale), Modifier.width(64.dp), textAlign = TextAlign.Center, style = MaterialTheme.typography.bodySmall.merge(TabularNumbers))
        Text(timeText(row.iftar, zone, locale), Modifier.width(64.dp), textAlign = TextAlign.Center, style = MaterialTheme.typography.bodySmall.merge(TabularNumbers))
    }
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
}

/** R3 — ro'za kuzatuvchisi: 30 katak, har kunga holat; qazo soni yuqorida. */
@Composable
fun FastTrackerRoute(viewModel: RamadanDaysViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val s = state ?: return
    var chosen by remember { mutableStateOf<RamadanDayRow?>(null) }
    Column(Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(stringResource(R.string.ramadan_fasts_title), style = MaterialTheme.typography.titleMedium)
        Text(stringResource(R.string.ramadan_fast_qada, s.fastQada), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
        Text(stringResource(R.string.ramadan_fast_hint), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        s.rows.chunked(6).forEach { week ->
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                week.forEach { row ->
                    FastCell(row, Modifier.weight(1f)) { chosen = row }
                }
                repeat(6 - week.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
    chosen?.let { row ->
        AlertDialog(
            onDismissRequest = { chosen = null },
            title = { Text(stringResource(R.string.ramadan_fast_day, row.number)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    FastOption(stringResource(R.string.ramadan_fast_fasted), row.fast == FastStatus.FASTED) { viewModel.setFast(row, FastStatus.FASTED); chosen = null }
                    FastOption(stringResource(R.string.ramadan_fast_missed), row.fast == FastStatus.MISSED) { viewModel.setFast(row, FastStatus.MISSED); chosen = null }
                    FastOption(stringResource(R.string.ramadan_fast_excused), row.fast == FastStatus.EXCUSED) { viewModel.setFast(row, FastStatus.EXCUSED); chosen = null }
                    FastOption(stringResource(R.string.ramadan_fast_clear), row.fast == null) { viewModel.setFast(row, null); chosen = null }
                }
            },
            confirmButton = { TextButton(onClick = { chosen = null }) { Text(stringResource(R.string.ramadan_close)) } },
        )
    }
}

@Composable
private fun FastCell(row: RamadanDayRow, modifier: Modifier, onClick: () -> Unit) {
    val (bg, label) = when (row.fast) {
        FastStatus.FASTED -> MaterialTheme.colorScheme.primary to stringResource(R.string.ramadan_fast_fasted)
        FastStatus.MISSED -> MaterialTheme.colorScheme.error to stringResource(R.string.ramadan_fast_missed)
        FastStatus.EXCUSED -> MaterialTheme.colorScheme.tertiary to stringResource(R.string.ramadan_fast_excused)
        null -> MaterialTheme.colorScheme.surfaceVariant to stringResource(R.string.ramadan_fast_unset)
    }
    Column(
        modifier.height(64.dp).clip(RoundedCornerShape(10.dp)).background(bg).clickable(onClick = onClick)
            .semantics { contentDescription = "${row.number}: $label" },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text("${row.number}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Text(label, style = MaterialTheme.typography.labelSmall, maxLines = 1)
    }
}

@Composable
private fun FastOption(label: String, selected: Boolean, onClick: () -> Unit) {
    FilterChip(selected = selected, onClick = onClick, label = { Text(label) })
}

/** R4 — saharlik va iftor duolari: arabcha matn, transliteratsiya, tarjima, nusxa olish. */
@Composable
fun DuasRoute() {
    val clipboard = LocalClipboardManager.current
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        DuaCard(stringResource(R.string.dua_suhoor_title), stringResource(R.string.dua_suhoor_arabic), stringResource(R.string.dua_suhoor_translit), stringResource(R.string.dua_suhoor_translation)) {
            clipboard.setText(AnnotatedString(stringResource(R.string.dua_suhoor_arabic)))
        }
        DuaCard(stringResource(R.string.dua_iftar_title), stringResource(R.string.dua_iftar_arabic), stringResource(R.string.dua_iftar_translit), stringResource(R.string.dua_iftar_translation)) {
            clipboard.setText(AnnotatedString(stringResource(R.string.dua_iftar_arabic)))
        }
    }
}

@Composable
private fun DuaCard(title: String, arabic: String, translit: String, translation: String, onCopy: () -> Unit) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall)
            Text(arabic, style = MaterialTheme.typography.headlineSmall.copy(fontSize = 24.sp), textAlign = TextAlign.End, modifier = Modifier.fillMaxWidth())
            Text(translit, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(translation, style = MaterialTheme.typography.bodyMedium)
            TextButton(onClick = onCopy) { Text(stringResource(R.string.dua_copy)) }
        }
    }
}

/** R5 — Ramazon xatmi: bugungi juz, kunlik norma, 30 juzlik progress; "O'qishni boshlash" mus'hafga o'tadi. */
@Composable
fun KhatmRoute(onOpenMushaf: () -> Unit, viewModel: KhatmViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val s = state ?: return
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(stringResource(R.string.ramadan_khatm_title), style = MaterialTheme.typography.titleMedium)
        Text(stringResource(R.string.khatm_progress, s.juzDone), style = MaterialTheme.typography.bodyLarge)
        LinearProgressIndicator(progress = { s.juzDone / 30f }, Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)))
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(stringResource(R.string.khatm_today, s.todayTarget), style = MaterialTheme.typography.titleSmall)
                Text(stringResource(R.string.khatm_daily, "%.1f".format(s.dailyJuz), s.remainingDays), style = MaterialTheme.typography.bodySmall)
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            OutlinedButton(onClick = { viewModel.adjustJuz(-1) }) { Text("−") }
            Text("${s.juzDone} / 30", style = MaterialTheme.typography.titleMedium.merge(TabularNumbers))
            OutlinedButton(onClick = { viewModel.adjustJuz(1) }) { Text("+") }
        }
        Button(onClick = onOpenMushaf, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.khatm_open_mushaf)) }
    }
}

/** R6 — Ramazon sozlamalari. */
@Composable
fun RamadanSettingsRoute(viewModel: RamadanSettingsViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val s = state ?: return
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text(stringResource(R.string.ramadan_imsak_offset, s.settings.imsakOffsetMin), style = MaterialTheme.typography.bodyLarge)
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf(0, 5, 10, 15, 20).forEach { m ->
                FilterChip(selected = s.settings.imsakOffsetMin == m, onClick = { viewModel.setImsakOffset(m) }, label = { Text("$m") })
            }
        }
        SwitchRow(stringResource(R.string.ramadan_suhoor_reminder), s.settings.suhoorOn) { viewModel.setSuhoorOn(it) }
        Text(stringResource(R.string.ramadan_suhoor_minutes, s.settings.suhoorMinBefore), style = MaterialTheme.typography.bodySmall)
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf(15, 30, 45, 60).forEach { m ->
                FilterChip(selected = s.settings.suhoorMinBefore == m, onClick = { viewModel.setSuhoorMinBefore(m) }, label = { Text("$m") })
            }
        }
        SwitchRow(stringResource(R.string.ramadan_iftar_reminder), s.settings.iftarOn) { viewModel.setIftarOn(it) }
        Text(stringResource(R.string.ramadan_iftar_minutes, s.settings.iftarMinBefore), style = MaterialTheme.typography.bodySmall)
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf(0, 5, 10, 15).forEach { m ->
                FilterChip(selected = s.settings.iftarMinBefore == m, onClick = { viewModel.setIftarMinBefore(m) }, label = { Text("$m") })
            }
        }
        SwitchRow(stringResource(R.string.ramadan_qadr_reminder), s.settings.qadrOn) { viewModel.setQadrOn(it) }
        HorizontalDivider()
        Text(stringResource(R.string.ramadan_hijri_adjust, signed(s.hijriAdjustDays)), style = MaterialTheme.typography.bodyLarge)
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf(-1, 0, 1).forEach { d ->
                FilterChip(selected = s.hijriAdjustDays == d, onClick = { viewModel.setHijriAdjust(d) }, label = { Text(signed(d)) })
            }
        }
        Text(stringResource(R.string.ramadan_official_check), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

private fun signed(d: Int): String = if (d > 0) "+$d" else "$d"

@Composable
private fun SwitchRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

/** Namoz tabidagi katta karta (TZ 11-bo'lim): Ramazondan 15 kun oldin va Ramazon davomida. */
@Composable
fun RamadanEntryCard(onOpen: () -> Unit, viewModel: RamadanHomeViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val s = state ?: return
    if (!viewModel.cardVisible(s)) return
    Card(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp).clickable(onClick = onOpen),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.DarkMode, null)
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.ramadan_card_title), style = MaterialTheme.typography.titleMedium)
            }
            if (s.phase == RamadanPhase.BEFORE) {
                Text(stringResource(R.string.ramadan_starts_in, s.daysUntilStart.toInt()), style = MaterialTheme.typography.bodyMedium)
            } else {
                Text(stringResource(R.string.ramadan_day_title, s.dayNumber ?: 0), style = MaterialTheme.typography.bodyMedium)
                Text(
                    stringResource(R.string.ramadan_card_iftar, timeText(s.iftarAt, s.zone), countdownText(s.now, s.iftarAt?.takeIf { it.isAfter(s.now) })),
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}
