package com.copitoB.mypos.presentation.ui.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
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
import com.copitoB.mypos.domain.entities.ICartItem
import com.copitoB.mypos.domain.valueobjects.PaymentMethod
import com.copitoB.mypos.generated.resources.Res
import com.copitoB.mypos.generated.resources.action_add
import com.copitoB.mypos.generated.resources.action_checkout
import com.copitoB.mypos.generated.resources.action_clear
import com.copitoB.mypos.generated.resources.action_remove_item
import com.copitoB.mypos.generated.resources.cart_empty_hint
import com.copitoB.mypos.generated.resources.checkout_line_edit_title
import com.copitoB.mypos.generated.resources.error_invalid_number
import com.copitoB.mypos.generated.resources.cart_qty_price
import com.copitoB.mypos.generated.resources.cart_subtotal
import com.copitoB.mypos.generated.resources.cart_subtotal_item
import com.copitoB.mypos.generated.resources.checkout_cart_title_purchase
import com.copitoB.mypos.generated.resources.checkout_cart_title_sale
import com.copitoB.mypos.generated.resources.checkout_mode_purchase
import com.copitoB.mypos.generated.resources.checkout_mode_sale
import com.copitoB.mypos.generated.resources.checkout_payment_method
import org.jetbrains.compose.resources.stringResource

enum class CartMode { SALE, PURCHASE }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegistroScreen(
  mode: CartMode,
  saleItems: List<ICartItem>,
  purchaseItems: List<ICartItem>,
  onModeChange: (CartMode) -> Unit,
  onAddItem: () -> Unit,
  onRemoveItem: (Int) -> Unit,
  onClearSale: () -> Unit,
  onClearPurchase: () -> Unit,
  onCheckoutSale: (PaymentMethod, com.copitoB.mypos.domain.valueobjects.Money?, Double) -> Unit,
  onCheckoutPurchase: (PaymentMethod, Double) -> Unit,
  discount: Double,
  onDiscountChange: (Double) -> Unit
) {
  val isSale = mode == CartMode.SALE
  val items = if (isSale) saleItems else purchaseItems
  val subtotal = items.sumOf { it.subtotal }
  var showPayment by remember { mutableStateOf(false) }

  Scaffold(
    topBar = {
      TopAppBar(
        title = {
          Text(
            stringResource(
              if (isSale) Res.string.checkout_cart_title_sale
              else Res.string.checkout_cart_title_purchase
            )
          )
        }
      )
    }
  ) { innerPadding ->
    Column(
      modifier = Modifier.fillMaxSize().padding(innerPadding).padding(horizontal = 16.dp)
    ) {
      SegmentedModeSelector(
        mode = mode,
        onModeChange = onModeChange
      )

      if (items.isEmpty()) {
        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
          Text(
            text = stringResource(Res.string.cart_empty_hint),
            style = MaterialTheme.typography.bodyLarge
          )
        }
      } else {
        LazyColumn(
          modifier = Modifier.weight(1f),
          contentPadding = PaddingValues(vertical = 8.dp),
          verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          itemsIndexed(items, key = { index, _ -> index }) { index, item ->
            CartItemRow(
              item = item,
              onRemove = { onRemoveItem(index) }
            )
          }
        }
      }

      Text(
        text = stringResource(Res.string.cart_subtotal, subtotal),
        style = MaterialTheme.typography.titleLarge,
        modifier = Modifier.padding(vertical = 8.dp)
      )

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        TextButton(onClick = { if (isSale) onClearSale() else onClearPurchase() }) {
          Text(stringResource(Res.string.action_clear))
        }
        TextButton(onClick = onAddItem) {
          Text(stringResource(Res.string.action_add))
        }
        TextButton(
          onClick = { showPayment = true },
          enabled = items.isNotEmpty(),
          modifier = Modifier.weight(1f)
        ) {
          Text(stringResource(Res.string.action_checkout))
        }
      }
    }
  }

  if (showPayment) {
    PaymentDialog(
      isSale = isSale,
      total = subtotal,
      discount = discount,
      onConfirm = { method, received, appliedDiscount ->
        onDiscountChange(appliedDiscount)
        if (isSale) onCheckoutSale(method, received, appliedDiscount)
        else onCheckoutPurchase(method, appliedDiscount)
        showPayment = false
      },
      onDismiss = { showPayment = false }
    )
  }
}

@Composable
private fun SegmentedModeSelector(
  mode: CartMode,
  onModeChange: (CartMode) -> Unit
) {
  Row(
    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
    horizontalArrangement = Arrangement.spacedBy(8.dp)
  ) {
    androidx.compose.material3.FilterChip(
      selected = mode == CartMode.SALE,
      onClick = { onModeChange(CartMode.SALE) },
      label = { Text(stringResource(Res.string.checkout_mode_sale)) }
    )
    androidx.compose.material3.FilterChip(
      selected = mode == CartMode.PURCHASE,
      onClick = { onModeChange(CartMode.PURCHASE) },
      label = { Text(stringResource(Res.string.checkout_mode_purchase)) }
    )
  }
}

@Composable
private fun CartItemRow(
  item: ICartItem,
  onRemove: () -> Unit
) {
  Card(
    modifier = Modifier.fillMaxWidth(),
    elevation = androidx.compose.material3.CardDefaults.cardElevation(defaultElevation = 2.dp)
  ) {
    Row(
      modifier = Modifier.fillMaxWidth().padding(12.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      Column(modifier = Modifier.weight(1f)) {
        Text(text = item.productName, style = MaterialTheme.typography.titleSmall)
        Text(
          text = stringResource(Res.string.cart_qty_price, item.quantity, item.unitPrice),
          style = MaterialTheme.typography.bodySmall
        )
        Text(
          text = stringResource(Res.string.cart_subtotal_item, item.subtotal),
          style = MaterialTheme.typography.bodySmall
        )
      }
      TextButton(onClick = onRemove) {
        Text(stringResource(Res.string.action_remove_item))
      }
    }
  }
}