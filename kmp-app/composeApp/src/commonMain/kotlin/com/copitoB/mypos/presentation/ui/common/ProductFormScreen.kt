package com.copitoB.mypos.presentation.ui.common

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import com.copitoB.mypos.domain.entities.Product
import com.copitoB.mypos.domain.entities.ProductInput
import com.copitoB.mypos.generated.resources.Res
import com.copitoB.mypos.generated.resources.action_cancel
import com.copitoB.mypos.generated.resources.action_save
import com.copitoB.mypos.generated.resources.content_desc_back
import com.copitoB.mypos.generated.resources.label_current_stock
import com.copitoB.mypos.generated.resources.label_min_stock
import com.copitoB.mypos.generated.resources.label_name
import com.copitoB.mypos.generated.resources.label_purchase_price
import com.copitoB.mypos.generated.resources.label_selling_price
import com.copitoB.mypos.generated.resources.label_sku
import com.copitoB.mypos.generated.resources.screen_edit_product
import com.copitoB.mypos.generated.resources.screen_new_product
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductFormScreen(
  product: Product? = null,
  onSave: (ProductInput) -> Unit,
  onCancel: () -> Unit
) {
  var name by remember { mutableStateOf(product?.name ?: "") }
  var sku by remember { mutableStateOf(product?.sku?.value ?: "") }
  var sellingPrice by remember { mutableStateOf(product?.sellingPrice?.amount?.toString() ?: "") }
  var purchasePrice by remember { mutableStateOf(product?.purchasePrice?.amount?.toString() ?: "") }
  var currentStock by remember { mutableStateOf(product?.currentStock?.value?.toString() ?: "") }
  var minStock by remember { mutableStateOf(product?.minStock?.value?.toString() ?: "") }
  var categoryId by remember { mutableStateOf(product?.categoryId ?: 1L) }

  Scaffold(
    topBar = {
      TopAppBar(
        title = { Text(stringResource(if (product == null) Res.string.screen_new_product else Res.string.screen_edit_product)) },
        navigationIcon = {
          IconButton(onClick = onCancel) {
            Icon(
              imageVector = Icons.AutoMirrored.Filled.ArrowBack,
              contentDescription = stringResource(Res.string.content_desc_back)
            )
          }
        }
      )
    }
  ) { innerPadding ->
    Column(
      modifier = Modifier.fillMaxSize().padding(innerPadding)
    ) {
      TextField(
        value = name,
        onValueChange = { name = it },
        label = { Text(stringResource(Res.string.label_name)) },
        modifier = Modifier.fillMaxWidth()
      )
      TextField(
        value = sku,
        onValueChange = { sku = it },
        label = { Text(stringResource(Res.string.label_sku)) },
        modifier = Modifier.fillMaxWidth()
      )
      TextField(
        value = sellingPrice,
        onValueChange = { sellingPrice = it },
        label = { Text(stringResource(Res.string.label_selling_price)) },
        modifier = Modifier.fillMaxWidth()
      )
      TextField(
        value = purchasePrice,
        onValueChange = { purchasePrice = it },
        label = { Text(stringResource(Res.string.label_purchase_price)) },
        modifier = Modifier.fillMaxWidth()
      )
      TextField(
        value = currentStock,
        onValueChange = { currentStock = it },
        label = { Text(stringResource(Res.string.label_current_stock)) },
        modifier = Modifier.fillMaxWidth()
      )
      TextField(
        value = minStock,
        onValueChange = { minStock = it },
        label = { Text(stringResource(Res.string.label_min_stock)) },
        modifier = Modifier.fillMaxWidth()
      )

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End
      ) {
        Button(onClick = {
          onSave(ProductInput(
            categoryId = categoryId,
            sku = sku.takeIf { it.isNotEmpty() },
            name = name,
            sellingPrice = sellingPrice,
            purchasePrice = purchasePrice,
            currentStock = currentStock,
            minStock = minStock,
            imageUri = null
          ))
        }) {
          Text(stringResource(Res.string.action_save))
        }
        TextButton(onClick = onCancel) {
          Text(stringResource(Res.string.action_cancel))
        }
      }
    }
  }
}