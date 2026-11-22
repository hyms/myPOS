package com.copitoB.mypos.presentation.ui.security

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.copitoB.mypos.generated.resources.Res
import com.copitoB.mypos.generated.resources.action_cancel
import com.copitoB.mypos.generated.resources.action_confirm
import com.copitoB.mypos.generated.resources.pin_confirm
import com.copitoB.mypos.generated.resources.pin_current
import com.copitoB.mypos.generated.resources.pin_enter
import com.copitoB.mypos.generated.resources.pin_error_generic
import com.copitoB.mypos.generated.resources.pin_incorrect
import com.copitoB.mypos.generated.resources.pin_lock_title
import com.copitoB.mypos.generated.resources.pin_mismatch
import com.copitoB.mypos.generated.resources.pin_new
import com.copitoB.mypos.generated.resources.pin_too_short
import com.copitoB.mypos.generated.resources.settings_pin_change
import com.copitoB.mypos.generated.resources.settings_pin_remove
import org.jetbrains.compose.resources.stringResource

private fun pinErrorKey(raw: String?): String? = when (raw) {
  "incorrect_pin" -> "incorrect"
  "pin_error" -> "error"
  null -> null
  else -> raw
}

@Composable
private fun PinErrorLabel(errorKey: String?) {
  when (errorKey) {
    "incorrect" -> Text(stringResource(Res.string.pin_incorrect), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
    "error" -> Text(stringResource(Res.string.pin_error_generic), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
    "pin_too_short" -> Text(stringResource(Res.string.pin_too_short), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
    "pin_mismatch" -> Text(stringResource(Res.string.pin_mismatch), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
    else -> Unit
  }
}

@Composable
fun PinGateDialog(
  title: String,
  pin: String,
  onPinChange: (String) -> Unit,
  error: String?,
  onConfirm: () -> Unit,
  onDismiss: () -> Unit
) {
  androidx.compose.material3.AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text(title) },
    text = {
      Column {
        PinForm(
          label = stringResource(Res.string.pin_enter),
          pin = pin,
          onPinChange = onPinChange
        )
        PinErrorLabel(pinErrorKey(error))
      }
    },
    confirmButton = {
      TextButton(onClick = onConfirm) { Text(stringResource(Res.string.action_confirm)) }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) { Text(stringResource(Res.string.action_cancel)) }
    }
  )
}

@Composable
fun ChangePinDialog(
  hasPin: Boolean,
  currentPin: String,
  newPin: String,
  confirmPin: String,
  error: String?,
  onCurrentChange: (String) -> Unit,
  onNewChange: (String) -> Unit,
  onConfirmChange: (String) -> Unit,
  onConfirm: () -> Unit,
  onRemove: () -> Unit,
  onDismiss: () -> Unit
) {
  androidx.compose.material3.AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text(stringResource(Res.string.settings_pin_change)) },
    text = {
      Column {
        if (hasPin) {
          PinForm(
            label = stringResource(Res.string.pin_current),
            pin = currentPin,
            onPinChange = onCurrentChange
          )
        }
        PinForm(
          label = stringResource(Res.string.pin_new),
          pin = newPin,
          onPinChange = onNewChange
        )
        PinForm(
          label = stringResource(Res.string.pin_confirm),
          pin = confirmPin,
          onPinChange = onConfirmChange
        )
        PinErrorLabel(pinErrorKey(error))
      }
    },
    confirmButton = {
      TextButton(onClick = onConfirm) { Text(stringResource(Res.string.action_confirm)) }
    },
    dismissButton = {
      Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        if (hasPin) {
          TextButton(onClick = onRemove) { Text(stringResource(Res.string.settings_pin_remove)) }
        }
        TextButton(onClick = onDismiss) { Text(stringResource(Res.string.action_cancel)) }
      }
    }
  )
}

@Composable
fun PinLockOverlay(
  visible: Boolean,
  pin: String,
  onPinChange: (String) -> Unit,
  error: String?,
  onUnlock: () -> Unit
) {
  if (!visible) return
  Surface(modifier = Modifier.fillMaxSize()) {
    Column(
      modifier = Modifier.fillMaxSize().systemBarsPadding().padding(24.dp),
      verticalArrangement = Arrangement.Center,
      horizontalAlignment = Alignment.CenterHorizontally
    ) {
      Text(
        text = stringResource(Res.string.pin_lock_title),
        style = MaterialTheme.typography.headlineSmall,
        modifier = Modifier.padding(bottom = 16.dp)
      )
      PinForm(
        label = stringResource(Res.string.pin_enter),
        pin = pin,
        onPinChange = onPinChange
      )
      PinErrorLabel(pinErrorKey(error))
      Button(
        onClick = onUnlock,
        modifier = Modifier.fillMaxWidth().padding(top = 16.dp)
      ) {
        Text(stringResource(Res.string.action_confirm))
      }
    }
  }
}