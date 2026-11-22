package com.copitoB.mypos.presentation.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import com.copitoB.mypos.config.AppConfig
import com.copitoB.mypos.presentation.ui.common.rememberReducedMotion
import com.materialkolor.material3.rememberDynamicColorScheme

val LocalReducedMotion = compositionLocalOf { false }

@Composable
fun AppTheme(content: @Composable () -> Unit) {
  val isDark = if (AppConfig.IS_DARK_FOLLOWS_SYSTEM) isSystemInDarkTheme() else false
  val colorScheme = rememberDynamicColorScheme(
    seedColor = AppConfig.SEED_COLOR,
    isDark = isDark,
    style = AppConfig.PALETTE_STYLE,
  )
  CompositionLocalProvider(LocalReducedMotion provides rememberReducedMotion()) {
    MaterialTheme(colorScheme = colorScheme, content = content)
  }
}