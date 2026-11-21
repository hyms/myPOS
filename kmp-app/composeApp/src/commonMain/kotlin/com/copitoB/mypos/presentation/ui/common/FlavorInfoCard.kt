package com.copitoB.mypos.presentation.ui.common

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.copitoB.mypos.config.AppConfig
import com.copitoB.mypos.generated.resources.Res
import com.copitoB.mypos.generated.resources.settings_ads_mode
import com.copitoB.mypos.generated.resources.settings_ads_note
import com.copitoB.mypos.generated.resources.settings_build_id
import com.copitoB.mypos.generated.resources.settings_flavor_title
import org.jetbrains.compose.resources.stringResource

@Composable
fun FlavorInfoCard() {
  Card(
    modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
  ) {
    Column(modifier = Modifier.padding(16.dp)) {
      Text(
        text = stringResource(Res.string.settings_flavor_title),
        style = MaterialTheme.typography.titleMedium
      )
      Text(
        text = stringResource(Res.string.settings_build_id, AppConfig.APPLICATION_ID),
        style = MaterialTheme.typography.bodyMedium
      )
      Text(
        text = stringResource(Res.string.settings_ads_mode, AppConfig.ADS_ENABLED.toString()),
        style = MaterialTheme.typography.bodyMedium
      )
      Text(
        text = stringResource(Res.string.settings_ads_note),
        style = MaterialTheme.typography.bodySmall,
        modifier = Modifier.padding(top = 4.dp)
      )
    }
  }
}
