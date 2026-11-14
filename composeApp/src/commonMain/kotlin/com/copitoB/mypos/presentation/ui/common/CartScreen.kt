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
import com.copitoB.mypos.domain.entities.ICartItem

@Composable
fun CartScreen(
  items: List<ICartItem>,
  onAddItem: () -> Unit,
  onRemoveItem: (Int) -> Unit,
  onCheckout: () -> Unit,
  onClear: () -> Unit
) {
  val subtotal = items.sumOf { it.subtotal }
  var discount by remember { mutableStateOf(0.0) }
  var received by remember { mutableStateOf("") }

  Column(
    modifier = Modifier.fillMaxSize().padding(16.dp)
  ) {
    androidx.compose.material3.Text(
      text = "Carrito de Compras",
      style = MaterialTheme.typography.headlineMedium
    )

    androidx.compose.foundation.lazy.LazyColumn(
      modifier = Modifier.weight(1f)
    ) {
      items(items, key = { it.productId.toString() }) { item ->
        androidx.compose.material3.Card(
          modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
        ) {
          androidx.compose.foundation.layout.Row(
            modifier = androidx.compose.foundation.layout.RowScopeModifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
          ) {
            Column(modifier = Modifier.weight(1f)) {
              Text(text = item.productName, style = MaterialTheme.typography.titleMedium)
              Text(text = "Qty: ${item.quantity} x ${item.unitPrice}", style = MaterialTheme.typography.bodySmall)
              Text(text = "Subtotal: ${item.subtotal}", style = MaterialTheme.typography.bodySmall)
            }
            androidx.compose.material3.IconButton(onClick = { onRemoveItem(items.indexOf(item)) }) {
              androidx.compose.material3.Icon(
                imageVector = androidx.compose.material.icons.Icons.Default.Delete,
                contentDescription = "Eliminar item"
              )
            }
          }
        }
      }
    }

    androidx.compose.material3.Text(
      text = "Subtotal: $subtotal",
      style = MaterialTheme.typography.titleLarge
    )

    androidx.compose.material3.TextField(
      value = discount.toString(),
      onValueChange = { discount = it.toDoubleOrNull() ?: 0.0 },
      label = { Text("Descuento") },
      modifier = Modifier.fillMaxWidth()
    )

    androidx.compose.material3.TextField(
      value = received,
      onValueChange = { received = it },
      label = { Text("Efectivo recibido") },
      modifier = Modifier.fillMaxWidth()
    )

    androidx.compose.foundation.layout.Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = androidx.compose.foundation.layout.Arrangement.End
    ) {
      Button(onClick = onClear) {
        Text(text = "Limpiar")
      }
      Button(onClick = onCheckout) {
        Text(text = "Pagar")
      }
    }
  }
}
