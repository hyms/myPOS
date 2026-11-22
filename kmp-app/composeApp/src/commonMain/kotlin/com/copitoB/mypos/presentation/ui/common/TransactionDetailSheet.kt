package com.copitoB.mypos.presentation.ui.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.copitoB.mypos.domain.entities.SaleTransaction
import com.copitoB.mypos.domain.entities.TransactionDetail
import com.copitoB.mypos.domain.valueobjects.Currency
import com.copitoB.mypos.domain.valueobjects.Money
import com.copitoB.mypos.generated.resources.Res
import com.copitoB.mypos.generated.resources.accumulator_detail_amount
import com.copitoB.mypos.generated.resources.accumulator_detail_date
import com.copitoB.mypos.generated.resources.accumulator_detail_description
import com.copitoB.mypos.generated.resources.accumulator_detail_payment
import com.copitoB.mypos.generated.resources.accumulator_detail_products_header
import com.copitoB.mypos.generated.resources.accumulator_detail_quantity
import com.copitoB.mypos.generated.resources.accumulator_detail_title
import com.copitoB.mypos.generated.resources.accumulator_detail_type_expense
import com.copitoB.mypos.generated.resources.accumulator_detail_type_purchase
import com.copitoB.mypos.generated.resources.accumulator_detail_type_sale
import com.copitoB.mypos.generated.resources.accumulator_revert_button
import com.copitoB.mypos.generated.resources.accumulator_revert_confirm_message
import com.copitoB.mypos.generated.resources.accumulator_revert_confirm_title
import com.copitoB.mypos.generated.resources.accumulator_revert_pin_title
import com.copitoB.mypos.generated.resources.action_cancel
import com.copitoB.mypos.generated.resources.action_confirm
import org.jetbrains.compose.resources.stringResource
import java.math.BigDecimal

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionDetailSheet(
  transaction: SaleTransaction,
  details: List<TransactionDetail>,
  productNames: Map<Long, String>,
  showPinGate: Boolean,
  pin: String,
  pinError: String?,
  showConfirm: Boolean,
  reverting: Boolean,
  onDismiss: () -> Unit,
  onRevert: () -> Unit,
  onPinChange: (String) -> Unit,
  onPinConfirm: () -> Unit,
  onPinCancel: () -> Unit,
  onConfirmRevert: () -> Unit,
  onCancelConfirm: () -> Unit
) {
  ModalBottomSheet(onDismissRequest = onDismiss) {
    Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
      Text(text = stringResource(Res.string.accumulator_detail_title), style = MaterialTheme.typography.titleLarge)
      Text(
        text = stringResource(
          when (transaction.type) {
            "SALE" -> Res.string.accumulator_detail_type_sale
            "PURCHASE" -> Res.string.accumulator_detail_type_purchase
            else -> Res.string.accumulator_detail_type_expense
          }
        ),
        style = MaterialTheme.typography.labelLarge
      )
      Text(
        text = stringResource(
          Res.string.accumulator_detail_amount,
          transaction.totalAmount.amount.setScale(2, java.math.RoundingMode.HALF_UP)
        ),
        style = MaterialTheme.typography.bodyMedium,
        modifier = Modifier.padding(top = 8.dp)
      )
      Text(
        text = stringResource(Res.string.accumulator_detail_date, formatTimestampOrRaw(transaction.registeredAt)),
        style = MaterialTheme.typography.bodyMedium
      )
      Text(
        text = stringResource(Res.string.accumulator_detail_payment, paymentMethodLabel(transaction.paymentMethod)),
        style = MaterialTheme.typography.bodyMedium
      )
      transaction.detail?.let {
        Text(
          text = stringResource(Res.string.accumulator_detail_description, it),
          style = MaterialTheme.typography.bodyMedium
        )
      }
      if (details.isNotEmpty()) {
        Text(
          text = stringResource(Res.string.accumulator_detail_products_header),
          style = MaterialTheme.typography.titleMedium,
          modifier = Modifier.padding(top = 12.dp)
        )
        details.forEach { detail ->
          Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
            horizontalArrangement = Arrangement.SpaceBetween
          ) {
            Text(
              text = productNames[detail.productId] ?: detail.productId.toString(),
              style = MaterialTheme.typography.bodyMedium
            )
            Text(
              text = stringResource(
                Res.string.accumulator_detail_quantity,
                detail.quantity,
                Money(BigDecimal(detail.unitPrice.toString()), Currency.Bs).amount.setScale(2, java.math.RoundingMode.HALF_UP)
              ),
              style = MaterialTheme.typography.bodyMedium
            )
          }
        }
      }
      Button(
        onClick = onRevert,
        enabled = !reverting,
        modifier = Modifier.fillMaxWidth().padding(top = 16.dp)
      ) {
        if (reverting) {
          CircularProgressIndicator(modifier = Modifier.padding(4.dp), strokeWidth = 2.dp)
        } else {
          Text(stringResource(Res.string.accumulator_revert_button))
        }
      }
    }
  }

  if (showPinGate) {
    AlertDialog(
      onDismissRequest = onPinCancel,
      title = { Text(stringResource(Res.string.accumulator_revert_pin_title)) },
      text = {
        Column {
          OutlinedTextField(
            value = pin,
            onValueChange = onPinChange,
            label = { Text(stringResource(Res.string.accumulator_revert_pin_title)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
          )
          pinError?.let {
            Text(
              text = it,
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.error
            )
          }
        }
      },
      confirmButton = {
        TextButton(onClick = onPinConfirm) { Text(stringResource(Res.string.action_confirm)) }
      },
      dismissButton = {
        TextButton(onClick = onPinCancel) { Text(stringResource(Res.string.action_cancel)) }
      }
    )
  }

  if (showConfirm) {
    ConfirmDialog(
      title = stringResource(Res.string.accumulator_revert_confirm_title),
      message = stringResource(Res.string.accumulator_revert_confirm_message),
      confirmLabel = stringResource(Res.string.accumulator_revert_button),
      dismissLabel = stringResource(Res.string.action_cancel),
      onConfirm = onConfirmRevert,
      onDismiss = onCancelConfirm
    )
  }
}