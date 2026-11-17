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
import androidx.compose.material.icons.filled.ArrowBack
import com.copitoB.mypos.domain.entities.Product
import com.copitoB.mypos.domain.entities.ProductInput

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
        title = { Text(if (product == null) "Nuevo Producto" else "Editar Producto") },
        navigationIcon = {
          IconButton(onClick = onCancel) {
            Icon(
              imageVector = Icons.Default.ArrowBack,
              contentDescription = "Volver"
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
        label = { Text("Nombre") },
        modifier = Modifier.fillMaxWidth()
      )
      TextField(
        value = sku,
        onValueChange = { sku = it },
        label = { Text("SKU") },
        modifier = Modifier.fillMaxWidth()
      )
      TextField(
        value = sellingPrice,
        onValueChange = { sellingPrice = it },
        label = { Text("Precio de Venta") },
        modifier = Modifier.fillMaxWidth()
      )
      TextField(
        value = purchasePrice,
        onValueChange = { purchasePrice = it },
        label = { Text("Precio de Compra") },
        modifier = Modifier.fillMaxWidth()
      )
      TextField(
        value = currentStock,
        onValueChange = { currentStock = it },
        label = { Text("Stock Actual") },
        modifier = Modifier.fillMaxWidth()
      )
      TextField(
        value = minStock,
        onValueChange = { minStock = it },
        label = { Text("Stock Mínimo") },
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
          Text("Guardar")
        }
        TextButton(onClick = onCancel) {
          Text("Cancelar")
        }
      }
    }
  }
}