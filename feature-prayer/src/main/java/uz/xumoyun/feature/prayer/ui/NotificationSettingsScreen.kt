package uz.xumoyun.feature.prayer.ui

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import uz.xumoyun.core.ui.theme.AppTheme
import uz.xumoyun.core.ui.theme.NamozHifzPalette
import uz.xumoyun.feature.prayer.R
import uz.xumoyun.feature.prayer.domain.AlertMode
import uz.xumoyun.feature.prayer.domain.Prayer
import uz.xumoyun.feature.prayer.domain.PrayerAlert
import uz.xumoyun.feature.prayer.notifications.ExactAlarms
import uz.xumoyun.feature.prayer.notifications.NotificationAccess

/** Sozlamalar → Bildirishnomalar (sarlavhani ilova navigatsiyasi chizadi). */
@Composable
fun NotificationSettingsRoute(viewModel: NotificationSettingsViewModel = hiltViewModel()) {
    val loaded by viewModel.alerts.collectAsStateWithLifecycle()
    val alerts = loaded ?: return
    val context = LocalContext.current
    val access = rememberNotificationAccessState()
    // POST_NOTIFICATIONS (Android 13+): so'raladi; rad etilgan bo'lsa tizim sozlamasi ochiladi.
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (!granted) NotificationAccess.openSettings(context)
    }
    NotificationSettingsContent(
        alerts = alerts,
        access = access,
        onAllowNotifications = {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !NotificationAccess.permissionGranted(context)) {
                permission.launch(Manifest.permission.POST_NOTIFICATIONS)
            } else {
                NotificationAccess.openSettings(context)
            }
        },
        onAllowExact = { ExactAlarms.openSettings(context) },
        onMode = viewModel::setMode,
        onMinutesBefore = viewModel::setMinutesBefore,
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun NotificationSettingsContent(
    alerts: Map<Prayer, PrayerAlert>,
    access: NotificationAccessState,
    onAllowNotifications: () -> Unit,
    onAllowExact: () -> Unit,
    onMode: (Prayer, AlertMode) -> Unit,
    onMinutesBefore: (Prayer, Int) -> Unit,
) {
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (!access.notificationsEnabled) NotificationsBlockedCard(onAllow = onAllowNotifications)
        if (!access.exactAlarms) ExactAlarmCard(onAllow = onAllowExact)

        Prayer.entries.forEach { prayer ->
            val alert = alerts.getValue(prayer)
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Icon(prayerIcon(prayer), null, tint = MaterialTheme.colorScheme.primary)
                        Text(stringResource(prayerName(prayer)), style = MaterialTheme.typography.titleMedium)
                    }
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        AlertMode.entries.forEach { mode ->
                            FilterChip(
                                selected = alert.mode == mode,
                                onClick = { onMode(prayer, mode) },
                                label = { Text(stringResource(alertModeName(mode))) },
                            )
                        }
                    }
                    if (alert.enabled) {
                        Text(stringResource(R.string.remind_before), style = MaterialTheme.typography.bodySmall)
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            PrayerAlert.MINUTES_BEFORE_OPTIONS.forEach { m ->
                                FilterChip(
                                    selected = alert.minutesBefore == m,
                                    onClick = { onMinutesBefore(prayer, m) },
                                    label = {
                                        Text(if (m == 0) stringResource(R.string.alert_mode_off) else stringResource(R.string.minutes_short, m))
                                    },
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
    AppTheme(NamozHifzPalette) {
        Surface {
            NotificationSettingsContent(
                alerts = Prayer.entries.associateWith {
                    if (it == Prayer.FAJR) PrayerAlert(AlertMode.SOUND, 10) else PrayerAlert.default(it)
                },
                access = NotificationAccessState(notificationsEnabled = true, exactAlarms = false),
                onAllowNotifications = {}, onAllowExact = {}, onMode = { _, _ -> }, onMinutesBefore = { _, _ -> },
            )
        }
    }
}
