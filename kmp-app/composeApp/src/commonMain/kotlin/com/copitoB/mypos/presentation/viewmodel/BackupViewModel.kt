package com.copitoB.mypos.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.copitoB.mypos.application.backup.BackupSource
import com.copitoB.mypos.application.backup.ExportBackupUseCase
import com.copitoB.mypos.application.backup.ImportBackupUseCase
import com.copitoB.mypos.application.backup.ImportReport
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class BackupViewModel(
  private val exportBackup: ExportBackupUseCase,
  private val importBackup: ImportBackupUseCase
) : ViewModel() {

  private val _exporting = MutableStateFlow(false)
  val exporting: StateFlow<Boolean> = _exporting.asStateFlow()

  private val _importing = MutableStateFlow(false)
  val importing: StateFlow<Boolean> = _importing.asStateFlow()

  private val _lastExportPath = MutableStateFlow<String?>(null)
  val lastExportPath: StateFlow<String?> = _lastExportPath.asStateFlow()

  private val _report = MutableStateFlow<ImportReport?>(null)
  val report: StateFlow<ImportReport?> = _report.asStateFlow()

  private val _error = MutableStateFlow<String?>(null)
  val error: StateFlow<String?> = _error.asStateFlow()

  fun export(onResult: (String?) -> Unit = {}) {
    viewModelScope.launch {
      _exporting.value = true
      _error.value = null
      try {
        val path = exportBackup()
        _lastExportPath.value = path
        onResult(path)
      } catch (e: Exception) {
        _error.value = e.message ?: "export_error"
      } finally {
        _exporting.value = false
      }
    }
  }

  fun import(source: BackupSource, pin: String, onResult: (Boolean) -> Unit = {}) {
    viewModelScope.launch {
      _importing.value = true
      _error.value = null
      try {
        val report = importBackup(source, pin)
        _report.value = report
        onResult(true)
      } catch (e: Exception) {
        _error.value = e.message ?: "import_error"
        onResult(false)
      } finally {
        _importing.value = false
      }
    }
  }

  fun clearReport() {
    _report.value = null
  }

  fun clearError() {
    _error.value = null
  }
}