package com.copitoB.mypos.presentation.ui.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.copitoB.mypos.domain.entities.UserSettings
import com.copitoB.mypos.generated.resources.Res
import com.copitoB.mypos.generated.resources.action_refresh
import com.copitoB.mypos.generated.resources.backup_import_confirm
import com.copitoB.mypos.generated.resources.label_biometric
import com.copitoB.mypos.generated.resources.loading
import com.copitoB.mypos.generated.resources.screen_settings
import com.copitoB.mypos.generated.resources.section_backup
import com.copitoB.mypos.generated.resources.section_general
import com.copitoB.mypos.generated.resources.section_security
import com.copitoB.mypos.generated.resources.settings_backup_title
import com.copitoB.mypos.generated.resources.settings_currency_title
import com.copitoB.mypos.generated.resources.settings_page_size
import com.copitoB.mypos.generated.resources.settings_pin_change
import com.copitoB.mypos.generated.resources.settings_pin_set
import com.copitoB.mypos.generated.resources.settings_pin_title
import com.copitoB.mypos.presentation.ui.security.ChangePinDialog
import com.copitoB.mypos.presentation.ui.security.PinGateDialog
import com.copitoB.mypos.presentation.viewmodel.BackupViewModel
import com.copitoB.mypos.presentation.viewmodel.PinViewModel
import org.jetbrains.compose.resources.stringResource

@Composable
fun SettingsScreen(
  settings: UserSettings?,
  pinViewModel: PinViewModel,
  backupViewModel: BackupViewModel,
  onCurrencyChange: (String) -> Unit,
  onPageSizeChange: (Int) -> Unit,
  onBiometricChange: (Boolean) -> Unit,
  onRefresh: () -> Unit,
  onImportFilePicked: (com.copitoB.mypos.application.backup.BackupSource?, String) -> Unit
) {
  val hasPin by pinViewModel.hasPin.collectAsState()
  val pinError by pinViewModel.error.collectAsState()
  val backupError by backupViewModel.error.collectAsState()
  val importing by backupViewModel.importing.collectAsState()
  val exporting by backupViewModel.exporting.collectAsState()
  val lastExportPath by backupViewModel.lastExportPath.collectAsState()
  val report by backupViewModel.report.collectAsState()

  var showChangePin by remember { mutableStateOf(false) }
  var showImportPinGate by remember { mutableStateOf(false) }
  var currentPin by remember { mutableStateOf("") }
  var newPin by remember { mutableStateOf("") }
  var confirmPin by remember { mutableStateOf("") }
  var importPin by remember { mutableStateOf("") }
  var verifiedImportPin by remember { mutableStateOf("") }
  var changePinError by remember { mutableStateOf<String?>(null) }

  val importLauncher = rememberBackupFilePicker { source ->
    onImportFilePicked(source, verifiedImportPin)
  }

  LaunchedEffect(hasPin) {
    if (!hasPin) showChangePin = false
  }

  Column(
    modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)
  ) {
    Text(text = stringResource(Res.string.screen_settings), style = MaterialTheme.typography.headlineMedium)

    if (settings == null) {
      Text(
        text = stringResource(Res.string.loading),
        style = MaterialTheme.typography.bodyLarge,
        modifier = Modifier.padding(top = 16.dp)
      )
    } else {
      SectionHeader(stringResource(Res.string.section_general))
      Text(stringResource(Res.string.settings_currency_title), style = MaterialTheme.typography.bodyLarge)
      CurrencySelector(selected = settings.currencyCode, onSelect = onCurrencyChange)
      Text(
        stringResource(Res.string.settings_page_size),
        style = MaterialTheme.typography.bodyLarge,
        modifier = Modifier.padding(top = 8.dp)
      )
      PageSizeInput(value = settings.pageSize, onChange = onPageSizeChange)
      ToggleRow(
        label = stringResource(Res.string.label_biometric),
        checked = settings.biometricEnabled,
        onCheckedChange = onBiometricChange
      )

      SectionHeader(stringResource(Res.string.section_security))
      Text(stringResource(Res.string.settings_pin_title), style = MaterialTheme.typography.bodyLarge)
      Row(modifier = Modifier.fillMaxWidth().padding(top = 4.dp)) {
        Button(onClick = {
          currentPin = ""
          newPin = ""
          confirmPin = ""
          pinViewModel.clearError()
          showChangePin = true
        }) {
          Text(
            stringResource(
              if (hasPin) Res.string.settings_pin_change else Res.string.settings_pin_set
            )
          )
        }
      }

      SectionHeader(stringResource(Res.string.section_backup))
      Text(stringResource(Res.string.settings_backup_title), style = MaterialTheme.typography.bodyLarge)
      BackupActions(
        exporting = exporting,
        importing = importing,
        lastExportPath = lastExportPath,
        onExport = { backupViewModel.export() },
        onImport = {
          importPin = ""
          if (hasPin) {
            showImportPinGate = true
          } else {
            importLauncher()
          }
        }
      )
      report?.let {
        ImportReportLine(inserted = it.inserted, updated = it.updated, skipped = it.skipped)
        it.warnings.forEach { warning ->
          Text(
            text = warning,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.error
          )
        }
      }
      backupError?.let {
        Text(text = it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
      }

      FlavorInfoCard()
    }

    Button(
      onClick = onRefresh,
      modifier = Modifier.fillMaxWidth().padding(top = 24.dp)
    ) {
      Text(stringResource(Res.string.action_refresh))
    }
  }

  if (showChangePin) {
    ChangePinDialog(
      hasPin = hasPin,
      currentPin = currentPin,
      newPin = newPin,
      confirmPin = confirmPin,
      error = changePinError ?: pinError,
      onCurrentChange = { currentPin = it; changePinError = null },
      onNewChange = { newPin = it; changePinError = null },
      onConfirmChange = { confirmPin = it; changePinError = null },
      onConfirm = {
        when {
          newPin.length < 4 -> changePinError = "pin_too_short"
          newPin != confirmPin -> changePinError = "pin_mismatch"
          else -> {
            changePinError = null
            if (hasPin) {
              pinViewModel.changePin(currentPin, newPin) { ok -> if (ok) showChangePin = false }
            } else {
              pinViewModel.setInitialPin(newPin) { ok -> if (ok) showChangePin = false }
            }
          }
        }
      },
      onRemove = {
        pinViewModel.removePin(currentPin) { ok -> if (ok) showChangePin = false }
      },
      onDismiss = { showChangePin = false; changePinError = null }
    )
  }

  if (showImportPinGate) {
    PinGateDialog(
      title = stringResource(Res.string.backup_import_confirm),
      pin = importPin,
      onPinChange = { importPin = it },
      error = pinError,
      onConfirm = {
        pinViewModel.unlock(importPin) { ok ->
          if (ok) {
            verifiedImportPin = importPin
            importPin = ""
            showImportPinGate = false
            importLauncher()
          }
        }
      },
      onDismiss = { showImportPinGate = false; importPin = "" }
    )
  }
}

@Composable
private fun ToggleRow(
  label: String,
  checked: Boolean,
  onCheckedChange: (Boolean) -> Unit
) {
  Row(
    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically
  ) {
    Text(text = label, style = MaterialTheme.typography.bodyLarge)
    Switch(checked = checked, onCheckedChange = onCheckedChange)
  }
}