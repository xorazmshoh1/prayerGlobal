package uz.xumoyun.feature.prayer.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AlarmOff
import androidx.compose.material.icons.rounded.NotificationsOff
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import uz.xumoyun.feature.prayer.R
import uz.xumoyun.feature.prayer.notifications.ExactAlarms
import uz.xumoyun.feature.prayer.notifications.NotificationAccess

/** Ruxsatlar holati: ekranga qaytilganda (ON_RESUME) qayta tekshiriladi. */
data class NotificationAccessState(val notificationsEnabled: Boolean, val exactAlarms: Boolean)

@Composable
fun rememberNotificationAccessState(): NotificationAccessState {
    val context = LocalContext.current
    var state by remember {
        mutableStateOf(NotificationAccessState(NotificationAccess.enabled(context), ExactAlarms.canSchedule(context)))
    }
    LifecycleResumeEffect(Unit) {
        state = NotificationAccessState(NotificationAccess.enabled(context), ExactAlarms.canSchedule(context))
        onPauseOrDispose { }
    }
    return state
}

/**
 * TZ 8-bo'lim: SCHEDULE_EXACT_ALARM berilmagan — bildirishnomalar setWindow bilan kechikishi mumkinligini
 * tushuntiradi va "Signallar va eslatmalar" sozlamasiga olib o'tadi.
 */
@Composable
fun ExactAlarmCard(onAllow: () -> Unit, modifier: Modifier = Modifier, onLater: (() -> Unit)? = null) {
    PermissionCard(
        icon = Icons.Rounded.AlarmOff,
        title = stringResource(R.string.exact_alarm_title),
        text = stringResource(R.string.exact_alarm_text),
        onAllow = onAllow,
        onLater = onLater,
        modifier = modifier,
    )
}

/** Bildirishnomalar o'chirilgan yoki POST_NOTIFICATIONS berilmagan. */
@Composable
fun NotificationsBlockedCard(onAllow: () -> Unit, modifier: Modifier = Modifier) {
    PermissionCard(
        icon = Icons.Rounded.NotificationsOff,
        title = stringResource(R.string.notifications_blocked_title),
        text = stringResource(R.string.notifications_blocked_text),
        onAllow = onAllow,
        onLater = null,
        modifier = modifier,
    )
}

@Composable
private fun PermissionCard(
    icon: ImageVector,
    title: String,
    text: String,
    onAllow: () -> Unit,
    onLater: (() -> Unit)?,
    modifier: Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        ),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Icon(icon, null)
                Text(title, style = MaterialTheme.typography.titleMedium)
            }
            Text(text, style = MaterialTheme.typography.bodyMedium)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                if (onLater != null) TextButton(onClick = onLater) { Text(stringResource(R.string.later)) }
                Button(onClick = onAllow) { Text(stringResource(R.string.permission_allow)) }
            }
        }
    }
}
