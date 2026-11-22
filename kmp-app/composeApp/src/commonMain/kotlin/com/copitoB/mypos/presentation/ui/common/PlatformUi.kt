package com.copitoB.mypos.presentation.ui.common

import androidx.compose.runtime.Composable

@Composable
expect fun rememberReducedMotion(): Boolean

expect fun formatTimestamp(iso: String): String

fun formatTimestampOrRaw(iso: String?): String = if (iso.isNullOrBlank()) "" else formatTimestamp(iso)