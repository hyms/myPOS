package com.copitoB.mypos.presentation.ui.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.copitoB.mypos.domain.entities.UserSettings

@Composable
fun SettingsScreen(
  settings: UserSettings?,
  onAdEnabledChange: (Boolean) -> Unit,
  onBiometricChange: (Boolean) -> Unit,
  onRefresh: () -> Unit
) {
  Column(
    modifier = Modifier.fillMaxSize().padding(16.dp)
  ) {
    Text(text = "Ajustes", style = MaterialTheme.typography.headlineMedium)

    if (settings == null) {
      Text(
        text = "Cargando...",
        style = MaterialTheme.typography.bodyLarge,
        modifier = Modifier.padding(top = 16.dp)
      )
    } else {
      Text(
        text = "Moneda: ${settings.currencyCode}",
        style = MaterialTheme.typography.bodyLarge,
        modifier = Modifier.padding(top = 16.dp)
      )
      ToggleRow(
        label = "Mostrar anuncios",
        checked = settings.adEnabled,
        onCheckedChange = onAdEnabledChange
      )
      ToggleRow(
        label = "Biometría",
        checked = settings.biometricEnabled,
        onCheckedChange = onBiometricChange
      )
    }

    Button(
      onClick = onRefresh,
      modifier = Modifier.fillMaxWidth().padding(top = 24.dp)
    ) {
      Text("Actualizar")
    }
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
