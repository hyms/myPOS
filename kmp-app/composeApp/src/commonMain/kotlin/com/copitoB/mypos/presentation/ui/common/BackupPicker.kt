package com.copitoB.mypos.presentation.ui.common

import androidx.compose.runtime.Composable
import com.copitoB.mypos.application.backup.BackupSource

/**
 * Returns a launcher that opens the platform document picker and, on selection,
 * invokes [onResult] with a [BackupSource] (or `null` if cancelled).
 *
 * Android actual uses SAF `ACTION_OPEN_DOCUMENT`; other platforms are no-ops.
 */
@Composable
expect fun rememberBackupFilePicker(onResult: (BackupSource?) -> Unit): () -> Unit