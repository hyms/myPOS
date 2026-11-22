package com.copitoB.mypos.presentation.ui.common

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.UUID
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
actual fun rememberImagePicker(onResult: (String?) -> Unit): () -> Unit {
  val context = LocalContext.current
  val store = remember { ProductImageStore(context) }
  val launcher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.PickVisualMedia()
  ) { uri: Uri? ->
    if (uri == null) {
      onResult(null)
      return@rememberLauncherForActivityResult
    }
    CoroutineScope(Dispatchers.Main).launch {
      val path = withContext(Dispatchers.IO) {
        val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
          ?: return@withContext null
        val optimized = optimizeProductImage(bytes) ?: return@withContext null
        store.save(optimized)
      }
      onResult(path)
    }
  }
  return remember(launcher) {
    {
      launcher.launch(
        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
      )
    }
  }
}

actual fun optimizeProductImage(bytes: ByteArray): ByteArray? {
  val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
  BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
  if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

  val decoded = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
    ?: return null

  val maxSide = 400
  val base = decoded

  val scale = minOf(
    maxSide.toFloat() / base.width.toFloat(),
    maxSide.toFloat() / base.height.toFloat(),
    1.0f
  )
  val target = if (scale >= 1f) {
    base
  } else {
    val w = (base.width * scale).toInt().coerceAtLeast(1)
    val h = (base.height * scale).toInt().coerceAtLeast(1)
    Bitmap.createScaledBitmap(base, w, h, true)
  }

  val limit = 40 * 1024
  var quality = 70
  var out: ByteArray
  while (true) {
    val stream = ByteArrayOutputStream()
    target.compress(Bitmap.CompressFormat.JPEG, quality, stream)
    out = stream.toByteArray()
    if (out.size <= limit || quality <= 20) break
    quality -= 10
  }
  return out
}

actual class ProductImageStore actual constructor(context: Any?) {

  private val appContext: Context? = context as? Context

  actual fun save(bytes: ByteArray): String? {
    val ctx = appContext ?: return null
    val dir = File(ctx.filesDir, "productos").apply { mkdirs() }
    val file = File(dir, "${UUID.randomUUID()}.jpg")
    file.writeBytes(bytes)
    return file.absolutePath
  }
}