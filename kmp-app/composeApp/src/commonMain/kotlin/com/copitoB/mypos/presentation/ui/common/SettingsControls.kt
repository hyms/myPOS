package com.copitoB.mypos.presentation.ui.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.copitoB.mypos.generated.resources.Res
import com.copitoB.mypos.generated.resources.error_invalid_number
import com.copitoB.mypos.generated.resources.settings_currency_bs
import com.copitoB.mypos.generated.resources.settings_currency_eur
import com.copitoB.mypos.generated.resources.settings_currency_usd
import com.copitoB.mypos.generated.resources.settings_page_size_hint
import org.jetbrains.compose.resources.stringResource

private val currencyOptions = listOf(
  Triple("Bs", Res.string.settings_currency_bs, "Bs"),
  Triple("$", Res.string.settings_currency_usd, "$"),
  Triple("€", Res.string.settings_currency_eur, "€")
)

@Composable
fun CurrencySelector(
  selected: String,
  onSelect: (String) -> Unit
) {
  Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
      currencyOptions.forEach { (code, labelRes, _) ->
        FilterChip(
          selected = selected == code,
          onClick = { onSelect(code) },
          label = { Text(stringResource(labelRes)) }
        )
      }
    }
  }
}

@Composable
fun PageSizeInput(
  value: Int,
  onChange: (Int) -> Unit
) {
  var text by remember(value) { mutableStateOf(value.toString()) }
  var error by remember { mutableStateOf(false) }

  Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
    OutlinedTextField(
      value = text,
      onValueChange = { raw ->
        text = raw.filter { it.isDigit() }.take(3)
        val parsed = text.toIntOrNull()
        error = parsed == null || parsed !in 5..100
        if (parsed != null && parsed in 5..100) {
          onChange(parsed)
        }
      },
      label = { Text(stringResource(Res.string.settings_page_size_hint)) },
      keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
      isError = error,
      supportingText = {
        if (error) Text(stringResource(Res.string.error_invalid_number))
      },
      modifier = Modifier.fillMaxWidth()
    )
  }
}