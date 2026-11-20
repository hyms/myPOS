package com.copitoB.mypos.presentation.ui.common

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import com.copitoB.mypos.domain.entities.ICartItem
import com.copitoB.mypos.generated.resources.Res
import com.copitoB.mypos.generated.resources.action_checkout
import com.copitoB.mypos.generated.resources.action_clear
import com.copitoB.mypos.generated.resources.cart_qty_price
import com.copitoB.mypos.generated.resources.cart_subtotal
import com.copitoB.mypos.generated.resources.cart_subtotal_item
import com.copitoB.mypos.generated.resources.content_desc_remove_item
import com.copitoB.mypos.generated.resources.label_cash_received
import com.copitoB.mypos.generated.resources.label_discount
import com.copitoB.mypos.generated.resources.screen_cart
import org.jetbrains.compose.resources.stringResource

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
    Text(
      text = stringResource(Res.string.screen_cart),
      style = MaterialTheme.typography.headlineMedium
    )

    LazyColumn(
      modifier = Modifier.fillMaxSize(),
      contentPadding = PaddingValues(0.dp)
    ) {
      items(items, key = { it.productId.toString() }) { item ->
        Card(
          modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
        ) {
          Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Column(modifier = Modifier.weight(1f)) {
              Text(text = item.productName, style = MaterialTheme.typography.titleMedium)
              Text(text = stringResource(Res.string.cart_qty_price, item.quantity, item.unitPrice), style = MaterialTheme.typography.bodySmall)
              Text(text = stringResource(Res.string.cart_subtotal_item, item.subtotal), style = MaterialTheme.typography.bodySmall)
            }
            IconButton(onClick = { onRemoveItem(items.indexOf(item)) }) {
              Icon(
                imageVector = Icons.Default.Delete,
                contentDescription = stringResource(Res.string.content_desc_remove_item)
              )
            }
          }
        }
      }
    }

    Text(
      text = stringResource(Res.string.cart_subtotal, subtotal),
      style = MaterialTheme.typography.titleLarge
    )

    TextField(
      value = discount.toString(),
      onValueChange = { discount = it.toDoubleOrNull() ?: 0.0 },
      label = { Text(stringResource(Res.string.label_discount)) },
      modifier = Modifier.fillMaxWidth()
    )

    TextField(
      value = received,
      onValueChange = { received = it },
      label = { Text(stringResource(Res.string.label_cash_received)) },
      modifier = Modifier.fillMaxWidth()
    )

    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = androidx.compose.foundation.layout.Arrangement.End
    ) {
      Button(onClick = onClear) {
        Text(text = stringResource(Res.string.action_clear))
      }
      Button(onClick = onCheckout) {
        Text(text = stringResource(Res.string.action_checkout))
      }
    }
  }
}