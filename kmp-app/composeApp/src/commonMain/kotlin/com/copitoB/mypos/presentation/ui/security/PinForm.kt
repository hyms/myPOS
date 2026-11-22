package com.copitoB.mypos.presentation.ui.security

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp

@Composable
fun PinForm(
  label: String,
  pin: String,
  onPinChange: (String) -> Unit,
  error: String? = null,
  enabled: Boolean = true,
  modifier: Modifier = Modifier
) {
  Column(modifier = modifier.fillMaxWidth().padding(vertical = 4.dp)) {
    OutlinedTextField(
      value = pin,
      onValueChange = { raw -> onPinChange(raw.filter { it.isDigit() }.take(6)) },
      label = { Text(label) },
      singleLine = true,
      visualTransformation = PasswordVisualTransformation(),
      keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
      enabled = enabled,
      isError = error != null,
      supportingText = { if (error != null) Text(error) },
      modifier = Modifier.fillMaxWidth()
    )
  }
}

@Composable
fun PinErrorText(message: String?) {
  if (message != null) {
    Text(
      text = message,
      style = MaterialTheme.typography.bodySmall,
      color = MaterialTheme.colorScheme.error,
      modifier = Modifier.padding(top = 4.dp)
    )
  }
}