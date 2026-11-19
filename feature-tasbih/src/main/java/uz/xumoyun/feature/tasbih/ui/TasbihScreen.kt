package uz.xumoyun.feature.tasbih.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import uz.xumoyun.core.ui.components.ProgressRing
import uz.xumoyun.core.ui.haptics.vibrate
import uz.xumoyun.core.ui.theme.TabularNumbers
import uz.xumoyun.feature.tasbih.R
import uz.xumoyun.feature.tasbih.domain.DayTotal
import uz.xumoyun.feature.tasbih.domain.Dhikr
import uz.xumoyun.feature.tasbih.domain.TasbihCounter
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@Composable
fun TasbihRoute(viewModel: TasbihViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val history by viewModel.history.collectAsStateWithLifecycle()
    val context = LocalContext.current
    TasbihContent(
        state = state,
        history = history,
        onTap = {
            val reached = viewModel.tap()
            vibrate(context, if (reached) 250L else 25L)
        },
        onReset = viewModel::reset,
        onSelect = viewModel::select,
        onTarget = viewModel::setTarget,
    )
}

@Composable
fun TasbihContent(
    state: TasbihState,
    history: List<DayTotal>,
    onTap: () -> Unit,
    onReset: () -> Unit,
    onSelect: (Dhikr) -> Unit,
    onTarget: (Int) -> Unit,
) {
    var customDialog by rememberSaveable { mutableStateOf(false) }
    val config = LocalConfiguration.current
    // Hisoblagich tugmasi ekranning taxminan yarmini egallaydi.
    val ringSize = minOf(config.screenWidthDp * 0.8f, config.screenHeightDp * 0.45f).dp
    val dateFormat = remember { DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM) }

    LazyColumn(
        Modifier.fillMaxSize().statusBarsPadding(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            LazyRow(
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(Dhikr.entries) { d ->
                    FilterChip(selected = d == state.dhikr, onClick = { onSelect(d) }, label = { Text(stringResource(d.labelRes())) })
                }
            }
        }
        // Zikr matni: arabcha (tarjima qilinmaydi) va transliteratsiya.
        item {
            Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    stringResource(state.dhikr.arabicRes()),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                )
                Text(
                    stringResource(state.dhikr.labelRes()),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }
        }
        item {
            Box(Modifier.fillMaxWidth().height(ringSize + 24.dp), contentAlignment = Alignment.Center) {
                ProgressRing(
                    progress = TasbihCounter.progress(state.count, state.target),
                    size = ringSize,
                    stroke = 12.dp,
                    color = MaterialTheme.colorScheme.tertiary,
                ) {
                    Surface(
                        onClick = onTap,
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(ringSize * 0.82f),
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    "${state.count}",
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    fontSize = 64.sp,
                                    style = TabularNumbers,
                                )
                                if (state.target > 0) {
                                    Text(
                                        stringResource(R.string.tasbeh_rounds, TasbihCounter.rounds(state.count, state.target), state.target),
                                        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f),
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                TasbihCounter.PRESET_TARGETS.forEach { t ->
                    FilterChip(selected = state.target == t, onClick = { onTarget(t) }, label = { Text("$t") })
                }
                FilterChip(
                    selected = state.target !in TasbihCounter.PRESET_TARGETS,
                    onClick = { customDialog = true },
                    label = { Text(stringResource(R.string.tasbeh_custom)) },
                )
                TextButton(onClick = onReset) {
                    Icon(Icons.Rounded.Refresh, null)
                    Text(stringResource(R.string.reset))
                }
            }
        }
        item {
            Text(
                stringResource(R.string.tasbeh_today_total, state.todayTotal),
                style = MaterialTheme.typography.titleMedium.merge(TabularNumbers),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        item {
            Text(
                stringResource(R.string.tasbeh_history),
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            )
        }
        items(history) { day ->
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(day.date.format(dateFormat), style = MaterialTheme.typography.bodyLarge)
                Text("${day.total}", style = MaterialTheme.typography.bodyLarge.merge(TabularNumbers))
            }
        }
    }

    if (customDialog) {
        var text by remember { mutableStateOf(state.target.toString()) }
        AlertDialog(
            onDismissRequest = { customDialog = false },
            title = { Text(stringResource(R.string.tasbeh_target)) },
            text = {
                OutlinedTextField(
                    value = text,
                    onValueChange = { v -> text = v.filter(Char::isDigit).take(6) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    TasbihCounter.validTarget(text.toIntOrNull())?.let(onTarget)
                    customDialog = false
                }) { Text(stringResource(R.string.ok)) }
            },
            dismissButton = { TextButton(onClick = { customDialog = false }) { Text(stringResource(R.string.cancel)) } },
        )
    }
}

private fun Dhikr.labelRes(): Int = when (this) {
    Dhikr.SUBHANALLAH -> R.string.dhikr_subhanallah
    Dhikr.ALHAMDULILLAH -> R.string.dhikr_alhamdulillah
    Dhikr.ALLAHU_AKBAR -> R.string.dhikr_allahuakbar
    Dhikr.LA_ILAHA_ILLALLAH -> R.string.dhikr_tahlil
    Dhikr.ASTAGHFIRULLAH -> R.string.dhikr_istighfar
    Dhikr.SALAWAT -> R.string.dhikr_salawat
}

private fun Dhikr.arabicRes(): Int = when (this) {
    Dhikr.SUBHANALLAH -> R.string.dhikr_subhanallah_ar
    Dhikr.ALHAMDULILLAH -> R.string.dhikr_alhamdulillah_ar
    Dhikr.ALLAHU_AKBAR -> R.string.dhikr_allahuakbar_ar
    Dhikr.LA_ILAHA_ILLALLAH -> R.string.dhikr_tahlil_ar
    Dhikr.ASTAGHFIRULLAH -> R.string.dhikr_istighfar_ar
    Dhikr.SALAWAT -> R.string.dhikr_salawat_ar
}
