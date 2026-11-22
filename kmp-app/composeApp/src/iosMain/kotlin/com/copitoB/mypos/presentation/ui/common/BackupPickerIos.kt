package com.copitoB.mypos.presentation.ui.common

import androidx.compose.runtime.Composable
import com.copitoB.mypos.application.backup.BackupSource

@Composable
actual fun rememberBackupFilePicker(onResult: (BackupSource?) -> Unit): () -> Unit {
  return {}
}