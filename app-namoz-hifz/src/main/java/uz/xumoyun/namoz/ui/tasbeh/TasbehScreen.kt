package uz.xumoyun.namoz.ui.tasbeh

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import uz.xumoyun.core.common.TimeSource
import uz.xumoyun.core.ui.components.ProgressRing
import uz.xumoyun.core.ui.theme.TabularNumbers
import uz.xumoyun.namoz.R
import uz.xumoyun.namoz.data.TasbehSettings
import uz.xumoyun.namoz.data.db.TasbehDao
import uz.xumoyun.namoz.data.db.TasbehDay
import uz.xumoyun.namoz.ui.NamozPreview
import javax.inject.Inject

enum class Dhikr(@StringRes val arabic: Int, @StringRes val label: Int) {
    SUBHANALLAH(R.string.dhikr_subhanallah_ar, R.string.dhikr_subhanallah),
    ALHAMDULILLAH(R.string.dhikr_alhamdulillah_ar, R.string.dhikr_alhamdulillah),
    ALLAHU_AKBAR(R.string.dhikr_allahuakbar_ar, R.string.dhikr_allahuakbar),
    LA_ILAHA_ILLALLAH(R.string.dhikr_tahlil_ar, R.string.dhikr_tahlil),
    ASTAGHFIRULLAH(R.string.dhikr_istighfar_ar, R.string.dhikr_istighfar),
    SALAWAT(R.string.dhikr_salawat_ar, R.string.dhikr_salawat),
}

data class TasbehState(
    val dhikr: Dhikr = Dhikr.SUBHANALLAH,
    val count: Int = 0,
    val target: Int = 33,
    val todayTotal: Int = 0,
) {
    val progress: Float get() = if (target == 0) 0f else (count % target).toFloat() / target
    val rounds: Int get() = if (target == 0) 0 else count / target
}

@HiltViewModel
class TasbehViewModel @Inject constructor(
    private val dao: TasbehDao,
    private val settings: TasbehSettings,
    private val time: TimeSource,
) : ViewModel() {
    private val selected = MutableStateFlow(Dhikr.SUBHANALLAH)
    private val session = MutableStateFlow(0)
    private val today get() = time.today().toString()

    @OptIn(ExperimentalCoroutinesApi::class)
    val state: StateFlow<TasbehState> = combine(selected, session, settings.target, dao.observeDay(today)) { d, c, target, day ->
        TasbehState(d, c, target, day.sumOf { it.count })
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), TasbehState())

    /** Bosilganda: hisob +1, kunlik hisob saqlanadi. Maqsadga yetganda true qaytaradi. */
    fun tap(): Boolean {
        val next = session.value + 1
        session.value = next
        val d = selected.value
        viewModelScope.launch {
            val current = dao.count(today, d.name) ?: 0
            dao.upsert(TasbehDay(today, d.name, current + 1))
        }
        val target = state.value.target
        return target > 0 && next % target == 0
    }

    fun reset() { session.value = 0 }

    fun select(d: Dhikr) {
        selected.value = d
        session.value = 0
    }

    fun setTarget(target: Int) = viewModelScope.launch { settings.setTarget(target) }
}

@Composable
fun TasbehRoute(viewModel: TasbehViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val vibrator = remember { vibrator(context) }
    TasbehContent(
        state,
        onTap = {
            val reached = viewModel.tap()
            vibrate(vibrator, if (reached) 250L else 25L)
        },
        onReset = viewModel::reset,
        onSelect = viewModel::select,
        onTarget = viewModel::setTarget,
    )
}

@Composable
fun TasbehContent(
    state: TasbehState,
    onTap: () -> Unit,
    onReset: () -> Unit,
    onSelect: (Dhikr) -> Unit,
    onTarget: (Int) -> Unit,
) {
    var customDialog by rememberSaveable { mutableStateOf(false) }
    Column(
        Modifier.fillMaxSize().statusBarsPadding().padding(vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        LazyRow(contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(Dhikr.entries) { d ->
                FilterChip(selected = d == state.dhikr, onClick = { onSelect(d) }, label = { Text(stringResource(d.label)) })
            }
        }
        Text(stringResource(state.dhikr.arabic), style = MaterialTheme.typography.headlineMedium, textAlign = TextAlign.Center, modifier = Modifier.padding(horizontal = 16.dp))
        Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
            ProgressRing(progress = state.progress, size = 280.dp, stroke = 12.dp, color = MaterialTheme.colorScheme.tertiary) {
                Surface(
                    onClick = onTap,
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(230.dp),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("${state.count}", color = MaterialTheme.colorScheme.onPrimary, fontSize = 64.sp, style = TabularNumbers)
                            if (state.target > 0) {
                                Text(stringResource(R.string.tasbeh_rounds, state.rounds, state.target), color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f))
                            }
                        }
                    }
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            listOf(33, 99).forEach { t ->
                FilterChip(selected = state.target == t, onClick = { onTarget(t) }, label = { Text("$t") })
            }
            FilterChip(selected = state.target !in listOf(33, 99), onClick = { customDialog = true }, label = { Text(stringResource(R.string.tasbeh_custom)) })
            TextButton(onClick = onReset) {
                Icon(Icons.Rounded.Refresh, null)
                Text(stringResource(R.string.reset))
            }
        }
        Text(stringResource(R.string.tasbeh_today_total, state.todayTotal), color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                TextButton(onClick = { text.toIntOrNull()?.takeIf { it > 0 }?.let(onTarget); customDialog = false }) { Text(stringResource(R.string.ok)) }
            },
            dismissButton = { TextButton(onClick = { customDialog = false }) { Text(stringResource(R.string.cancel)) } },
        )
    }
}

private fun vibrator(context: Context): Vibrator? =
    if (Build.VERSION.SDK_INT >= 31) context.getSystemService(VibratorManager::class.java)?.defaultVibrator
    else @Suppress("DEPRECATION") (context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator)

private fun vibrate(vibrator: Vibrator?, ms: Long) {
    vibrator ?: return
    if (Build.VERSION.SDK_INT >= 26) vibrator.vibrate(VibrationEffect.createOneShot(ms, VibrationEffect.DEFAULT_AMPLITUDE))
    else @Suppress("DEPRECATION") vibrator.vibrate(ms)
}

@Preview(showBackground = true)
@Composable
private fun TasbehPreview() {
    NamozPreview { TasbehContent(TasbehState(Dhikr.SUBHANALLAH, 21, 33, 120), {}, {}, {}, {}) }
}
