package com.copitoB.mypos.presentation.ui.common

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
actual fun ProductPhotoPreview(path: String?, modifier: Modifier, size: Dp?) {
  val bitmap = remember(path) {
    path?.let { p ->
      runCatching { BitmapFactory.decodeFile(p)?.asImageBitmap() }.getOrNull()
    }
  }
  val sizeMod = if (size != null) Modifier.size(size) else Modifier
  Box(
    modifier = modifier
      .then(sizeMod)
      .clip(RoundedCornerShape(8.dp))
      .background(MaterialTheme.colorScheme.surfaceVariant)
  ) {
    if (bitmap != null) {
      Image(
        bitmap = bitmap,
        contentDescription = null,
        contentScale = ContentScale.Crop,
        modifier = Modifier.matchParentSize()
      )
    }
  }
}