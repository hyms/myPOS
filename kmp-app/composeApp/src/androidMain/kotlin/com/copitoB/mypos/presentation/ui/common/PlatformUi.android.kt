package com.copitoB.mypos.presentation.ui.common

import android.animation.ValueAnimator
import android.os.Build
import android.provider.Settings
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.util.Locale

private val DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm", Locale.getDefault())

@Composable
actual fun rememberReducedMotion(): Boolean {
  val context = LocalContext.current
  return rememberReducedMotionImpl(context)
}

private fun rememberReducedMotionImpl(context: android.content.Context): Boolean {
  return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
    !ValueAnimator.areAnimatorsEnabled()
  } else {
    val scale = Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f)
    scale == 0f
  }
}

actual fun formatTimestamp(iso: String): String {
  return try {
    Instant.parse(iso).atZone(ZoneId.systemDefault()).format(DATE_TIME_FORMATTER)
  } catch (e: DateTimeParseException) {
    iso.take(16).replace('T', ' ')
  }
}