package com.copitoB.mypos.presentation.ui.common

import androidx.compose.runtime.Composable

@Composable
actual fun rememberReducedMotion(): Boolean = false

actual fun formatTimestamp(iso: String): String = iso