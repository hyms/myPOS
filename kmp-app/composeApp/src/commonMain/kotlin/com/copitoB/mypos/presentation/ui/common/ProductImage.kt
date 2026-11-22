package com.copitoB.mypos.presentation.ui.common

import androidx.compose.runtime.Composable

/**
 * Launches the platform photo picker. On selection, the image is optimized
 * (400x400, JPEG 70%, <40 KB) and persisted in app storage; the returned
 * value is the stored absolute path, or `null` if cancelled or unsupported.
 */
@Composable
expect fun rememberImagePicker(onResult: (String?) -> Unit): () -> Unit

/**
 * Decodes, resizes to fit within 400x400 and recompresses as JPEG at 70%,
 * retrying lower qualities until the payload is below 40 KB.
 * Returns `null` when the bytes cannot be decoded.
 */
expect fun optimizeProductImage(bytes: ByteArray): ByteArray?

/**
 * Persists an already optimized image in `<filesDir>/productos/<uuid>.jpg`.
 * `context` is an Android `Context` passed as `Any?`; ignored elsewhere.
 */
expect class ProductImageStore(context: Any?) {
  fun save(bytes: ByteArray): String?
}