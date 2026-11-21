package com.copitoB.mypos.presentation.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import com.copitoB.mypos.config.AppConfig
import com.materialkolor.material3.rememberDynamicColorScheme

@Composable
fun AppTheme(content: @Composable () -> Unit) {
  val isDark = if (AppConfig.IS_DARK_FOLLOWS_SYSTEM) isSystemInDarkTheme() else false
  val colorScheme = rememberDynamicColorScheme(
    seedColor = AppConfig.SEED_COLOR,
    isDark = isDark,
    style = AppConfig.PALETTE_STYLE,
  )
  MaterialTheme(colorScheme = colorScheme, content = content)
}
