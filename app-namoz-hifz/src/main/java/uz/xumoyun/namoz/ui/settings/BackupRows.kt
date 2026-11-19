package uz.xumoyun.namoz.ui.settings

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Restore
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import uz.xumoyun.core.ui.components.SettingsRow
import uz.xumoyun.namoz.R
import uz.xumoyun.namoz.backup.BackupError
import java.io.IOException
import java.time.LocalDate

/** Zaxira fayli bu hajmdan katta bo'lsa o'qilmaydi (xotirani himoya qilish). */
private const val MAX_BACKUP_BYTES = 32 * 1024 * 1024

/**
 * Zaxira bo'limi (TZ 13-bosqich, 17-funksiya): eksport va import faqat SAF orqali tanlangan faylga.
 * Ma'lumot tarmoqqa yuborilmaydi. Import hamma ma'lumotni almashtiradi — avval tasdiqlash so'raladi.
 */
@Composable
fun BackupRows(viewModel: BackupViewModel) {
    val context = LocalContext.current
    val status by viewModel.status.collectAsStateWithLifecycle()
    var confirmRestore by rememberSaveable { mutableStateOf(false) }

    val createLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) viewModel.exportTo { text -> writeText(context, uri, text) }
    }
    val openLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) viewModel.restoreFrom { readText(context, uri) }
    }

    SettingsRow(
        stringResource(R.string.backup_export),
        icon = Icons.Rounded.Save,
        onClick = { createLauncher.launch("namoz-hifz-backup-${LocalDate.now()}.json") },
    )
    SettingsRow(
        stringResource(R.string.backup_restore),
        icon = Icons.Rounded.Restore,
        onClick = { confirmRestore = true },
    )
    Text(
        stringResource(R.string.backup_hint),
        style = MaterialTheme.typography.bodySmall,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
    )

    if (confirmRestore) {
        AlertDialog(
            onDismissRequest = { confirmRestore = false },
            title = { Text(stringResource(R.string.backup_restore_confirm_title)) },
            text = { Text(stringResource(R.string.backup_restore_confirm_text)) },
            confirmButton = {
                TextButton(onClick = {
                    confirmRestore = false
                    openLauncher.launch(arrayOf("application/json", "text/plain", "application/octet-stream"))
                }) { Text(stringResource(R.string.backup_restore_confirm_yes)) }
            },
            dismissButton = { TextButton(onClick = { confirmRestore = false }) { Text(stringResource(R.string.cancel)) } },
        )
    }

    when (val s = status) {
        BackupStatus.Exported -> ResultDialog(stringResource(R.string.backup_exported), viewModel::clearStatus)
        is BackupStatus.Restored -> ResultDialog(
            stringResource(R.string.backup_restored, s.summary.rows, s.summary.prefs),
            viewModel::clearStatus,
        )
        is BackupStatus.Failed -> ResultDialog(stringResource(errorText(s.error)), viewModel::clearStatus)
        else -> Unit
    }
}

@Composable
private fun ResultDialog(message: String, onClose: () -> Unit) {
    AlertDialog(
        onDismissRequest = onClose,
        text = { Text(message) },
        confirmButton = { TextButton(onClick = onClose) { Text(stringResource(R.string.close)) } },
    )
}

@StringRes
private fun errorText(e: BackupError): Int = when (e) {
    BackupError.NOT_A_BACKUP -> R.string.backup_err_not_backup
    BackupError.NEWER_VERSION, BackupError.NEWER_SCHEMA -> R.string.backup_err_newer
    BackupError.INVALID -> R.string.backup_err_invalid
    BackupError.IO -> R.string.backup_err_io
}

private suspend fun writeText(context: Context, uri: Uri, text: String) = withContext(Dispatchers.IO) {
    val out = context.contentResolver.openOutputStream(uri, "wt") ?: throw IOException("no output stream")
    out.use { it.write(text.toByteArray(Charsets.UTF_8)) }
}

private suspend fun readText(context: Context, uri: Uri): String = withContext(Dispatchers.IO) {
    val input = context.contentResolver.openInputStream(uri) ?: throw IOException("no input stream")
    val bytes = input.use { it.readBytes() }
    if (bytes.size > MAX_BACKUP_BYTES) throw IOException("backup too large")
    String(bytes, Charsets.UTF_8)
}
