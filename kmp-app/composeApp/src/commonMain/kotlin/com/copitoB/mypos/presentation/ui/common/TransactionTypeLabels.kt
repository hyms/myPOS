package com.copitoB.mypos.presentation.ui.common

import androidx.compose.runtime.Composable
import com.copitoB.mypos.generated.resources.Res
import com.copitoB.mypos.generated.resources.accumulator_detail_type_expense
import com.copitoB.mypos.generated.resources.accumulator_detail_type_purchase
import com.copitoB.mypos.generated.resources.accumulator_detail_type_sale
import org.jetbrains.compose.resources.stringResource

@Composable
fun transactionTypeLabel(type: String): String = stringResource(
  when (type) {
    "SALE" -> Res.string.accumulator_detail_type_sale
    "PURCHASE" -> Res.string.accumulator_detail_type_purchase
    else -> Res.string.accumulator_detail_type_expense
  }
)