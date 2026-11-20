package com.copitoB.mypos.presentation.ui.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.copitoB.mypos.domain.entities.FinancialSummary
import com.copitoB.mypos.domain.valueobjects.Money
import com.copitoB.mypos.generated.resources.Res
import com.copitoB.mypos.generated.resources.action_refresh
import com.copitoB.mypos.generated.resources.loading
import com.copitoB.mypos.generated.resources.screen_cash
import com.copitoB.mypos.generated.resources.summary_expenses
import com.copitoB.mypos.generated.resources.summary_net
import com.copitoB.mypos.generated.resources.summary_purchases
import com.copitoB.mypos.generated.resources.summary_sales
import org.jetbrains.compose.resources.stringResource

@Composable
fun CashSummaryScreen(
  summary: FinancialSummary?,
  onRefresh: () -> Unit
) {
  Column(
    modifier = Modifier.fillMaxSize().padding(16.dp)
  ) {
    Text(text = stringResource(Res.string.screen_cash), style = MaterialTheme.typography.headlineMedium)

    if (summary == null) {
      Text(
        text = stringResource(Res.string.loading),
        style = MaterialTheme.typography.bodyLarge,
        modifier = Modifier.padding(top = 16.dp)
      )
    } else {
      SummaryRow(stringResource(Res.string.summary_sales), summary.totalSales)
      SummaryRow(stringResource(Res.string.summary_purchases), summary.totalPurchases)
      SummaryRow(stringResource(Res.string.summary_expenses), summary.totalExpenses)
      SummaryRow(
        label = stringResource(Res.string.summary_net),
        amount = summary.netBalance,
        negative = summary.netBalanceIsNegative
      )
    }

    Button(
      onClick = onRefresh,
      modifier = Modifier.fillMaxWidth().padding(top = 24.dp)
    ) {
      Text(stringResource(Res.string.action_refresh))
    }
  }
}

@Composable
private fun SummaryRow(label: String, amount: Money, negative: Boolean = false) {
  Row(
    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
    horizontalArrangement = Arrangement.SpaceBetween
  ) {
    Text(text = label, style = MaterialTheme.typography.bodyLarge)
    Text(
      text = if (negative) "-${amount}" else amount.toString(),
      style = MaterialTheme.typography.bodyLarge,
      color = if (negative) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
    )
  }
}
