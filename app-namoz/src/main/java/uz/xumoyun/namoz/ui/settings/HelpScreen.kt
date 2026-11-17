package uz.xumoyun.namoz.ui.settings

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import uz.xumoyun.namoz.R
import uz.xumoyun.namoz.ui.NamozPreview

/** "Bildirishnomalar kelmayaptimi?" — avtoishga tushirish va batareya cheklovi bo'yicha yo'riqnoma. */
@Composable
fun HelpScreen() {
    val context = LocalContext.current
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(stringResource(R.string.help_intro), style = MaterialTheme.typography.bodyLarge)
        HelpCard(stringResource(R.string.help_battery_title), stringResource(R.string.help_battery_text))
        HelpCard(stringResource(R.string.help_xiaomi_title), stringResource(R.string.help_xiaomi_text))
        HelpCard(stringResource(R.string.help_huawei_title), stringResource(R.string.help_huawei_text))
        HelpCard(stringResource(R.string.help_samsung_title), stringResource(R.string.help_samsung_text))
        OutlinedButton(modifier = Modifier.fillMaxWidth(), onClick = {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${context.packageName}"))
            runCatching { context.startActivity(intent) }
        }) { Text(stringResource(R.string.open_app_settings)) }
        if (Build.VERSION.SDK_INT >= 23) {
            OutlinedButton(modifier = Modifier.fillMaxWidth(), onClick = {
                runCatching { context.startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)) }
            }) { Text(stringResource(R.string.open_battery_settings)) }
        }
    }
}

@Composable
private fun HelpCard(title: String, text: String) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(text, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun HelpPreview() { NamozPreview { HelpScreen() } }
