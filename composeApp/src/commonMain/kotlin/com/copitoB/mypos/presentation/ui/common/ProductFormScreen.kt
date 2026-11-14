package com.copitoB.mypos.presentation.ui.common

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.copitoB.mypos.App
import com.copitoB.mypos.domain.entities.Product
import com.copitoB.mypos.domain.entities.ProductInput

@Composable
fun ProductFormScreen(
  product: Product? = null,
  onSave: (ProductInput) -> Unit,
  onCancel: () -> Unit
) {
  val name by remember { mutableStateOf(product?.name ?: "") }
  val sku by remember { mutableStateOf(product?.sku?.value ?: "") }
  val sellingPrice by remember { mutableStateOf(product?.sellingPrice.amount.toString() ?: "") }
  val purchasePrice by remember { mutableStateOf(product?.purchasePrice.amount.toString() ?: "") }
  val currentStock by remember { mutableStateOf(product?.currentStock.value.toString() ?: "") }
  val minStock by remember { mutableStateOf(product?.minStock.value.toString() ?: "") }
  val categoryId by remember { mutableStateOf(product?.categoryId ?: 1L) }

  Scaffold(
    topBar = {
      TopAppBar(
        title = { Text(if (product == null) "Nuevo Producto" else "Editar Producto") },
        navigationIcon = {
          androidx.compose.material3.IconButton(onClick = onCancel) {
            androidx.compose.material3.Icon(
              imageVector = androidx.compose.material.icons.Icons.Default.ArrowBack,
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
      androidx.compose.material3.TextField(
        value = name,
        onValueChange = { name = it },
        label = { Text("Nombre") },
        modifier = Modifier.fillMaxWidth()
      )
      androidx.compose.material3.TextField(
        value = sku,
        onValueChange = { sku = it },
        label = { Text("SKU") },
        modifier = Modifier.fillMaxWidth()
      )
      androidx.compose.material3.TextField(
        value = sellingPrice,
        onValueChange = { sellingPrice = it },
        label = { Text("Precio de Venta") },
        modifier = Modifier.fillMaxWidth()
      )
      androidx.compose.material3.TextField(
        value = purchasePrice,
        onValueChange = { purchasePrice = it },
        label = { Text("Precio de Compra") },
        modifier = Modifier.fillMaxWidth()
      )
      androidx.compose.material3.TextField(
        value = currentStock,
        onValueChange = { currentStock = it },
        label = { Text("Stock Actual") },
        modifier = Modifier.fillMaxWidth()
      )
      androidx.compose.material3.TextField(
        value = minStock,
        onValueChange = { minStock = it },
        label = { Text("Stock Mínimo") },
        modifier = Modifier.fillMaxWidth()
      )

      RowButtons {
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

@Composable
fun RowButtons(content: @Composable () -> Unit) {
  androidx.compose.foundation.layout.Row(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = androidx.compose.foundation.layout.Arrangement.End
  ) {
    content()
  }
}
