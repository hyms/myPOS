package com.copitoB.mypos

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.Alignment

@Composable
fun App() {
  Surface(
    modifier = Modifier.fillMaxSize(),
    color = MaterialTheme.colorScheme.background
  ) {
    Text(
      text = "TPV Móvil Express - Hola Mundo",
      style = MaterialTheme.typography.titleLarge,
      modifier = Modifier.align(alignment = Alignment.Center)
    )
  }
}

@Preview(showBackground = true)
@Composable
fun AppPreview() {
  MaterialTheme {
    App()
  }
}
