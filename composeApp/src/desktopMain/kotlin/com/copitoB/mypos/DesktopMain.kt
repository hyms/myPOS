package com.copitoB.mypos

import androidx.compose.desktop.ui.application.Application
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowState
import androidx.compose.ui.window.WindowStateConfig

@Composable
fun DesktopApp() {
  Window(
    onCloseRequest = ::exitApplication,
    title = "TPV Móvil Express",
    state = WindowState(
      config = WindowStateConfig(size = IntSize(400, 800))
    )
  ) {
    MaterialTheme {
      Surface(modifier = Modifier.fillMaxSize()) {
        App()
      }
    }
  }
}

fun main() = Application(DesktopApp)
