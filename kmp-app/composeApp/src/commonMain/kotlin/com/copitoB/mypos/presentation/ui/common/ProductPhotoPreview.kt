package com.copitoB.mypos.presentation.ui.common

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp

/**
 * Renders a preview for a product photo stored at [path].
 * Android decodes the file into a bitmap; platforms without filesystem
 * access render a neutral placeholder box.
 *
 * When [size] is null the composable fills whatever geometry the caller
 * supplies via [modifier]; pass a size to force a fixed square.
 */
@Composable
expect fun ProductPhotoPreview(
  path: String?,
  modifier: Modifier = Modifier,
  size: Dp? = null
)