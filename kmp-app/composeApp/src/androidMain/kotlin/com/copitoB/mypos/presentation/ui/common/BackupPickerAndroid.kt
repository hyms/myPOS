package com.copitoB.mypos.presentation.ui.common

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.copitoB.mypos.application.backup.BackupFileIo
import com.copitoB.mypos.application.backup.BackupSource
import com.copitoB.mypos.application.backup.createBackupFileIo

@Composable
actual fun rememberBackupFilePicker(onResult: (BackupSource?) -> Unit): () -> Unit {
  val context = LocalContext.current
  val fileIo: BackupFileIo = remember { createBackupFileIo(context) }
  val launcher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.OpenDocument()
  ) { uri: Uri? ->
    onResult(if (uri != null) fileIo.load(uri) else null)
  }
  return remember(launcher) {
    {
      launcher.launch(
        arrayOf(
          "application/json",
          "application/octet-stream",
          "application/x-sqlite3",
          "application/vnd.sqlite3",
          "*/*"
        )
      )
    }
  }
}