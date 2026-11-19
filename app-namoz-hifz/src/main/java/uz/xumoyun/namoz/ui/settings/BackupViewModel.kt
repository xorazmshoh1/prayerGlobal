package uz.xumoyun.namoz.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import uz.xumoyun.namoz.backup.BackupError
import uz.xumoyun.namoz.backup.BackupException
import uz.xumoyun.namoz.backup.BackupService
import uz.xumoyun.namoz.backup.BackupSummary
import javax.inject.Inject

sealed interface BackupStatus {
    data object Idle : BackupStatus
    data object Working : BackupStatus
    data object Exported : BackupStatus
    data class Restored(val summary: BackupSummary) : BackupStatus
    data class Failed(val error: BackupError) : BackupStatus
}

/**
 * Zaxira eksport/import. Fayl I/O (SAF) UI tomonida: [exportTo] yozuvchi va [restoreFrom] o'quvchi
 * funksiyalarini oladi, shunda ViewModel Android'siz sinaladi.
 */
@HiltViewModel
class BackupViewModel @Inject constructor(
    private val service: BackupService,
) : ViewModel() {

    private val _status = MutableStateFlow<BackupStatus>(BackupStatus.Idle)
    val status: StateFlow<BackupStatus> = _status.asStateFlow()

    fun exportTo(write: suspend (String) -> Unit) = viewModelScope.launch {
        _status.value = BackupStatus.Working
        _status.value = try {
            write(service.export())
            BackupStatus.Exported
        } catch (e: BackupException) {
            BackupStatus.Failed(e.error)
        } catch (e: Exception) {
            BackupStatus.Failed(BackupError.IO)
        }
    }

    fun restoreFrom(read: suspend () -> String) = viewModelScope.launch {
        _status.value = BackupStatus.Working
        _status.value = try {
            BackupStatus.Restored(service.restore(read()))
        } catch (e: BackupException) {
            BackupStatus.Failed(e.error)
        } catch (e: Exception) {
            BackupStatus.Failed(BackupError.IO)
        }
    }

    fun clearStatus() {
        _status.value = BackupStatus.Idle
    }
}
