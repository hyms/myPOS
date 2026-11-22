package com.copitoB.mypos.presentation.ui.common

import androidx.compose.runtime.Composable

@Composable
actual fun rememberImagePicker(onResult: (String?) -> Unit): () -> Unit = {}

actual fun optimizeProductImage(bytes: ByteArray): ByteArray? = null

actual class ProductImageStore actual constructor(context: Any?) {
  actual fun save(bytes: ByteArray): String? = null
}