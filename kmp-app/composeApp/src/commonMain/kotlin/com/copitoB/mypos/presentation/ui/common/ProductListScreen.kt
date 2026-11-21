package com.copitoB.mypos.presentation.ui.common

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import com.copitoB.mypos.domain.entities.Product
import com.copitoB.mypos.generated.resources.Res
import com.copitoB.mypos.generated.resources.action_delete
import com.copitoB.mypos.generated.resources.content_desc_add_product
import com.copitoB.mypos.generated.resources.product_sku
import com.copitoB.mypos.generated.resources.product_sku_missing
import com.copitoB.mypos.generated.resources.product_stock
import com.copitoB.mypos.generated.resources.screen_products
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductListScreen(
  products: List<Product>,
  onProductClick: (Long) -> Unit,
  onAddProduct: () -> Unit,
  onDelete: (Long) -> Unit
) {
  Scaffold(
    topBar = {
      TopAppBar(
        title = { Text(stringResource(Res.string.screen_products)) },
        actions = {
          IconButton(onClick = onAddProduct) {
            Icon(
              imageVector = Icons.Default.Add,
              contentDescription = stringResource(Res.string.content_desc_add_product)
            )
          }
        }
      )
    },
    floatingActionButton = {
      androidx.compose.material3.FloatingActionButton(onClick = onAddProduct) {
        Icon(
          imageVector = Icons.Default.Add,
          contentDescription = stringResource(Res.string.content_desc_add_product)
        )
      }
    }
  ) { innerPadding ->
    LazyColumn(
      modifier = Modifier.fillMaxSize().padding(innerPadding),
      contentPadding = PaddingValues(16.dp)
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
    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
  ) {
    Row(
      modifier = Modifier.padding(16.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      Column(modifier = Modifier.fillMaxWidth()) {
        Text(text = product.name, style = MaterialTheme.typography.titleMedium)
        Text(text = stringResource(Res.string.product_sku, product.sku?.value ?: stringResource(Res.string.product_sku_missing)), style = MaterialTheme.typography.bodySmall)
        Text(
          text = stringResource(Res.string.product_stock, product.currentStock, product.minStock),
          style = MaterialTheme.typography.bodySmall,
          color = if (product.isBelowMinStock()) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
        )
      }
      Row {
        TextButton(onClick = onDelete) {
          Text(text = stringResource(Res.string.action_delete))
        }
      }
    }
  }
}