package uz.xumoyun.namoz.ui.settings

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import uz.xumoyun.namoz.R

/** Bizning boshqa ilovalarimiz (TZ 12): statik ro'yxat, har biri Play havolasi bilan. Reklama tarmog'i emas. */
data class OtherApp(val name: String, val playId: String)

object OtherApps {
    /** TODO: ilovalar nashr etilganda shu ro'yxat to'ldiriladi (hozircha bo'sh — ekranda izoh chiqadi). */
    val ALL: List<OtherApp> = emptyList()
    const val PLAY_BASE = "https://play.google.com/store/apps/details?id="
    const val APP_ID = "uz.xumoyun.namozhifz"
    val APP_URL: String get() = PLAY_BASE + APP_ID
}

fun shareApp(context: Context) {
    val text = context.getString(R.string.share_app_text, OtherApps.APP_URL)
    val intent = Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, text)
    context.startActivity(Intent.createChooser(intent, null).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
}

@Composable
fun OtherAppsScreen(openUrl: (String) -> Unit) {
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        if (OtherApps.ALL.isEmpty()) {
            Text(stringResource(R.string.other_apps_empty), style = MaterialTheme.typography.bodyMedium)
        } else {
            OtherApps.ALL.forEach { app ->
                Text(
                    app.name,
                    Modifier.fillMaxWidth().clickable { openUrl(OtherApps.PLAY_BASE + app.playId) }.padding(vertical = 12.dp),
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
        }
    }
}

fun openPlayLink(context: Context, url: String) {
    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
}
