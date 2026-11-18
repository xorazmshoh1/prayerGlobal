package uz.xumoyun.namoz.ui.settings

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import uz.xumoyun.namoz.R
import uz.xumoyun.namoz.data.AlertSound
import uz.xumoyun.namoz.data.PrayerAlert
import uz.xumoyun.namoz.domain.Prayer
import uz.xumoyun.namoz.ui.NamozPreview
import uz.xumoyun.namoz.ui.prayerName
import uz.xumoyun.namoz.ui.soundName

@Composable
fun NotificationSettingsRoute(viewModel: SettingsViewModel) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val alerts = state.config?.prefs?.alerts ?: return
    val context = LocalContext.current
    val alarmManager = context.getSystemService(android.app.AlarmManager::class.java)
    val exactMissing = Build.VERSION.SDK_INT >= 31 && !alarmManager.canScheduleExactAlarms()
    NotificationSettingsContent(
        alerts = alerts,
        exactAlarmMissing = exactMissing,
        onRequestExact = {
            if (Build.VERSION.SDK_INT >= 31) {
                context.startActivity(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.parse("package:${context.packageName}")))
            }
        },
        onChange = viewModel::setAlert,
    )
}

@Composable
fun NotificationSettingsContent(
    alerts: Map<Prayer, PrayerAlert>,
    exactAlarmMissing: Boolean,
    onRequestExact: () -> Unit,
    onChange: (Prayer, PrayerAlert) -> Unit,
) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        if (exactAlarmMissing) {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(stringResource(R.string.exact_alarm_needed), color = MaterialTheme.colorScheme.onErrorContainer)
                    Button(onClick = onRequestExact) { Text(stringResource(R.string.allow)) }
                }
            }
        }
        Prayer.entries.forEach { prayer ->
            val alert = alerts.getValue(prayer)
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(stringResource(prayerName(prayer)), style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                        Switch(checked = alert.enabled, onCheckedChange = { onChange(prayer, alert.copy(enabled = it)) })
                    }
                    if (alert.enabled) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            AlertSound.entries.forEach { s ->
                                FilterChip(selected = alert.sound == s, onClick = { onChange(prayer, alert.copy(sound = s)) }, label = { Text(stringResource(soundName(s))) })
                            }
                        }
                        Text(stringResource(R.string.remind_before), style = MaterialTheme.typography.bodySmall)
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf(0, 5, 10, 15, 30).forEach { m ->
                                FilterChip(
                                    selected = alert.minutesBefore == m,
                                    onClick = { onChange(prayer, alert.copy(minutesBefore = m)) },
                                    label = { Text(if (m == 0) stringResource(R.string.off) else stringResource(R.string.minutes_short, m)) },
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun NotificationSettingsPreview() {
    NamozPreview {
        NotificationSettingsContent(
            alerts = Prayer.entries.associateWith { PrayerAlert(it.isPrayer, AlertSound.AZAN, if (it == Prayer.FAJR) 10 else 0) },
            exactAlarmMissing = true, onRequestExact = {}, onChange = { _, _ -> },
        )
    }
}
