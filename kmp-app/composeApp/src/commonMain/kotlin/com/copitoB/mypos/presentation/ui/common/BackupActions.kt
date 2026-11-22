package com.copitoB.mypos.presentation.ui.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.copitoB.mypos.generated.resources.Res
import com.copitoB.mypos.generated.resources.action_export_backup
import com.copitoB.mypos.generated.resources.action_import_backup
import com.copitoB.mypos.generated.resources.backup_export_success
import com.copitoB.mypos.generated.resources.backup_exporting
import com.copitoB.mypos.generated.resources.backup_import_report
import com.copitoB.mypos.generated.resources.backup_import_success
import com.copitoB.mypos.generated.resources.backup_importing
import org.jetbrains.compose.resources.stringResource

@Composable
fun BackupActions(
  exporting: Boolean,
  importing: Boolean,
  lastExportPath: String?,
  onExport: () -> Unit,
  onImport: () -> Unit
) {
  Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
      Button(
        onClick = onExport,
        enabled = !exporting && !importing,
        modifier = Modifier.weight(1f)
      ) {
        Text(stringResource(Res.string.action_export_backup))
      }
      OutlinedButton(
        onClick = onImport,
        enabled = !exporting && !importing,
        modifier = Modifier.weight(1f)
      ) {
        Text(stringResource(Res.string.action_import_backup))
      }
    }
    if (exporting) {
      Text(
        text = stringResource(Res.string.backup_exporting),
        style = MaterialTheme.typography.bodySmall,
        modifier = Modifier.padding(top = 4.dp)
      )
    }
    if (importing) {
      Text(
        text = stringResource(Res.string.backup_importing),
        style = MaterialTheme.typography.bodySmall,
        modifier = Modifier.padding(top = 4.dp)
      )
    }
    lastExportPath?.let { path ->
      Text(
        text = stringResource(Res.string.backup_export_success, path),
        style = MaterialTheme.typography.bodySmall,
        modifier = Modifier.padding(top = 4.dp)
      )
    }
  }
}

@Composable
fun ImportReportLine(inserted: Int, updated: Int, skipped: Int) {
  Text(
    text = stringResource(Res.string.backup_import_report, inserted, updated, skipped),
    style = MaterialTheme.typography.bodySmall
  )
}