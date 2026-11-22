package com.copitoB.mypos.presentation.ui.common

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.copitoB.mypos.domain.entities.ICartItem
import com.copitoB.mypos.generated.resources.Res
import com.copitoB.mypos.generated.resources.action_confirm
import com.copitoB.mypos.generated.resources.cart_subtotal
import com.copitoB.mypos.generated.resources.checkout_payment_change
import com.copitoB.mypos.generated.resources.checkout_payment_insufficient
import com.copitoB.mypos.generated.resources.checkout_payment_method
import com.copitoB.mypos.generated.resources.error_invalid_number
import com.copitoB.mypos.generated.resources.label_cash_received
import com.copitoB.mypos.generated.resources.label_discount
import com.copitoB.mypos.generated.resources.action_cancel
import com.copitoB.mypos.generated.resources.action_save
import com.copitoB.mypos.generated.resources.checkout_line_edit_title
import com.copitoB.mypos.generated.resources.checkout_line_quantity
import com.copitoB.mypos.generated.resources.checkout_line_subtotal
import com.copitoB.mypos.generated.resources.checkout_payment_title_purchase
import com.copitoB.mypos.generated.resources.checkout_payment_title_sale
import org.jetbrains.compose.resources.stringResource

@Composable
fun LineItemDialog(
  item: ICartItem,
  onConfirm: (ICartItem) -> Unit,
  onDismiss: () -> Unit
) {
  var quantity by remember { mutableStateOf(item.quantity.toString()) }
  var error by remember { mutableStateOf(false) }
  val parsed = quantity.toDoubleOrNull()
  val valid = parsed != null && parsed > 0.0
  val subtotal = if (valid) parsed!! * item.unitPrice else 0.0

  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text(item.productName) },
    text = {
      Column {
        OutlinedTextField(
          value = quantity,
          onValueChange = {
            quantity = it
            val q = it.toDoubleOrNull()
            error = q == null || q <= 0.0
          },
          label = { Text(stringResource(Res.string.checkout_line_quantity)) },
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
          isError = error,
          singleLine = true,
          modifier = Modifier.fillMaxWidth()
        )
        if (error) {
          Text(
            text = stringResource(Res.string.error_invalid_number),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.error
          )
        }
        Text(
          text = stringResource(Res.string.checkout_line_subtotal, subtotal),
          style = MaterialTheme.typography.titleMedium,
          modifier = Modifier.padding(top = 8.dp)
        )
      }
    },
    confirmButton = {
      TextButton(
        enabled = valid,
        onClick = {
          val q = quantity.toDoubleOrNull() ?: return@TextButton
          onConfirm(
            com.copitoB.mypos.domain.entities.CartItemFactory.create(
              productId = item.productId,
              productName = item.productName,
              quantity = q,
              unitPrice = item.unitPrice
            )
          )
        }
      ) {
        Text(stringResource(Res.string.action_save))
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) {
        Text(stringResource(Res.string.action_cancel))
      }
    }
  )
}

@Composable
fun PaymentDialog(
  isSale: Boolean,
  total: Double,
  discount: Double,
  onConfirm: (paymentMethod: com.copitoB.mypos.domain.valueobjects.PaymentMethod, received: com.copitoB.mypos.domain.valueobjects.Money?, discount: Double) -> Unit,
  onDismiss: () -> Unit
) {
  var paymentMethod by remember { mutableStateOf(com.copitoB.mypos.domain.valueobjects.PaymentMethod.CASH) }
  var discountText by remember { mutableStateOf(if (discount > 0) discount.toString() else "") }
  var receivedText by remember { mutableStateOf("") }
  var change by remember { mutableStateOf<com.copitoB.mypos.domain.valueobjects.ChangeAmount?>(null) }

  val effectiveDiscount = discountText.toDoubleOrNull()?.coerceIn(0.0, total) ?: 0.0
  val finalTotal = (total - effectiveDiscount).coerceAtLeast(0.0)

  fun recomputeChange() {
    val received = receivedText.toDoubleOrNull()
    change = if (received != null && finalTotal > 0.0) {
      com.copitoB.mypos.domain.valueobjects.ChangeAmount(
        received = com.copitoB.mypos.domain.valueobjects.Money(
          java.math.BigDecimal(received.toString()),
          com.copitoB.mypos.domain.valueobjects.Currency.Bs
        ),
        total = com.copitoB.mypos.domain.valueobjects.Money(
          java.math.BigDecimal(finalTotal.toString()),
          com.copitoB.mypos.domain.valueobjects.Currency.Bs
        )
      )
    } else null
  }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = {
      Text(
        stringResource(
          if (isSale) Res.string.checkout_payment_title_sale
          else Res.string.checkout_payment_title_purchase
        )
      )
    },
    text = {
      Column {
        Text(stringResource(Res.string.checkout_payment_method), style = MaterialTheme.typography.labelLarge)
        Row(
          modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
          horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)
        ) {
          com.copitoB.mypos.domain.valueobjects.PaymentMethod.entries.forEach { method ->
            androidx.compose.material3.FilterChip(
              selected = paymentMethod == method,
              onClick = { paymentMethod = method },
              label = { Text(paymentMethodLabel(method)) }
            )
          }
        }
        OutlinedTextField(
          value = discountText,
          onValueChange = {
            discountText = it
            val d = it.toDoubleOrNull()
            if (d != null && d > total) discountText = total.toString()
          },
          label = { Text(stringResource(Res.string.label_discount)) },
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
          singleLine = true,
          modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
        )
        if (isSale) {
          OutlinedTextField(
            value = receivedText,
            onValueChange = {
              receivedText = it
              recomputeChange()
            },
            label = { Text(stringResource(Res.string.label_cash_received)) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            singleLine = true,
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
          )
          change?.let { c ->
            val color = if (c.isSufficient()) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.error
            val value = if (c.isSufficient()) c.change.amount else com.copitoB.mypos.domain.valueobjects.Money(
              java.math.BigDecimal(finalTotal.toString()).subtract(
                com.copitoB.mypos.domain.valueobjects.Money(
                  java.math.BigDecimal(receivedText.toDoubleOrNull()?.toString() ?: "0"),
                  com.copitoB.mypos.domain.valueobjects.Currency.Bs
                ).amount
              ),
              com.copitoB.mypos.domain.valueobjects.Currency.Bs
            ).amount
            Text(
              text = stringResource(
                if (c.isSufficient()) Res.string.checkout_payment_change
                else Res.string.checkout_payment_insufficient,
                value
              ),
              style = MaterialTheme.typography.titleMedium,
              color = color,
              modifier = Modifier.padding(top = 8.dp)
            )
          }
        }
        Text(
          text = stringResource(Res.string.cart_subtotal, finalTotal),
          style = MaterialTheme.typography.titleLarge,
          modifier = Modifier.padding(top = 8.dp)
        )
      }
    },
    confirmButton = {
      val sufficient = !isSale || change == null || change!!.isSufficient()
      TextButton(
        enabled = sufficient && discountText.isBlank().or(discountText.toDoubleOrNull()?.let { it <= total } ?: true),
        onClick = {
          val received = if (isSale) {
            receivedText.toDoubleOrNull()?.let {
              com.copitoB.mypos.domain.valueobjects.Money(
                java.math.BigDecimal(it.toString()),
                com.copitoB.mypos.domain.valueobjects.Currency.Bs
              )
            }
          } else null
          onConfirm(paymentMethod, received, effectiveDiscount)
        }
      ) {
        Text(stringResource(Res.string.action_confirm))
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) {
        Text(stringResource(Res.string.action_cancel))
      }
    }
  )
}