package com.copitoB.mypos.presentation.ui.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.copitoB.mypos.App
import com.copitoB.mypos.domain.entities.Product

@Composable
fun ProductListScreen(
  products: List<Product>,
  onProductClick: (Long) -> Unit,
  onAddProduct: () -> Unit,
  onDelete: (Long) -> Unit
) {
  var showFab by remember { mutableStateOf(true) }

  Scaffold(
    topBar = {
      TopAppBar(
        title = { Text("Productos") },
        actions = {
          IconButton(onClick = onAddProduct) {
            androidx.compose.material3.Icon(
              imageVector = androidx.compose.material.icons.Icons.Default.Add,
              contentDescription = "Agregar producto"
            )
          }
        }
      )
    },
    floatingActionButton = if (showFab) {
      androidx.compose.material3.FloatingActionButton(onClick = onAddProduct) {
        androidx.compose.material3.Icon(
          imageVector = androidx.compose.material.icons.Icons.Default.Add,
          contentDescription = "Agregar producto"
        )
      }
    } else null
  ) { innerPadding ->
    LazyColumn(
      modifier = Modifier.fillMaxSize().padding(innerPadding),
      contentPadding = androidx.compose.foundation.lazy.PaddingValues(16.dp)
    ) {
      items(products, key = { it.id.toString() }) { product ->
        ProductCard(
          product = product,
          onClick = { onProductClick(product.id!!) },
          onDelete = { onDelete(product.id!!) }
        )
      }
    }
  }
}

@Composable
fun ProductCard(
  product: Product,
  onClick: () -> Unit,
  onDelete: () -> Unit
) {
  Card(
    modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
    elevation = 2.dp
  ) {
    androidx.compose.foundation.layout.Row(
      modifier = Modifier.padding(16.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      Column(modifier = Modifier.weight(1f)) {
        Text(text = product.name, style = MaterialTheme.typography.titleMedium)
        Text(text = "SKU: ${product.sku?.value ?: "N/A"}", style = MaterialTheme.typography.bodySmall)
        Text(
          text = "Stock: ${product.currentStock} (mín: ${product.minStock})",
          style = MaterialTheme.typography.bodySmall,
          color = if (product.isBelowMinStock()) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
        )
      }
      Row {
        TextButton(onClick = onDelete) {
          Text(text = "Eliminar")
        }
      }
    }
  }
}
