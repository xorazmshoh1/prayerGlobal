package uz.xumoyun.feature.calendar.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.item
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import uz.xumoyun.feature.calendar.R
import uz.xumoyun.feature.calendar.notifications.IslamicEventNotifier

/** Islomiy sanalar sozlamalari (TZ 6-bo'lim, Taqvim tabidan ochiladi): yoqish va 1 kun oldin eslatma. */
@Composable
fun IslamicEventsRoute(viewModel: IslamicEventsViewModel = hiltViewModel()) {
    val rows by viewModel.rows.collectAsStateWithLifecycle()
    LazyColumn(Modifier.fillMaxSize()) {
        item {
            Text(
                stringResource(R.string.islamic_events_hint),
                Modifier.padding(16.dp),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        items(rows, key = { it.event.key }) { row ->
            Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(IslamicEventNotifier.nameRes(row.event)), Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
                    Switch(
                        checked = row.setting.enabled,
                        onCheckedChange = { viewModel.setEnabled(row.event, it) },
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        stringResource(R.string.islamic_remind_day_before),
                        Modifier.weight(1f),
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (row.setting.enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Switch(
                        checked = row.setting.remindDayBefore && row.setting.enabled,
                        enabled = row.setting.enabled,
                        onCheckedChange = { viewModel.setRemindDayBefore(row.event, it) },
                    )
                }
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        }
    }
}
