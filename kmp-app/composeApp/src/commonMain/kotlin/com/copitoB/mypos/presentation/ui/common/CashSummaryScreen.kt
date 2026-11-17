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

@Composable
fun CashSummaryScreen(
  summary: FinancialSummary?,
  onRefresh: () -> Unit
) {
  Column(
    modifier = Modifier.fillMaxSize().padding(16.dp)
  ) {
    Text(text = "Caja", style = MaterialTheme.typography.headlineMedium)

    if (summary == null) {
      Text(
        text = "Cargando...",
        style = MaterialTheme.typography.bodyLarge,
        modifier = Modifier.padding(top = 16.dp)
      )
    } else {
      SummaryRow("Ventas", summary.totalSales)
      SummaryRow("Compras", summary.totalPurchases)
      SummaryRow("Gastos", summary.totalExpenses)
      SummaryRow(
        label = "Neto",
        amount = summary.netBalance,
        negative = summary.netBalanceIsNegative
      )
    }

    Button(
      onClick = onRefresh,
      modifier = Modifier.fillMaxWidth().padding(top = 24.dp)
    ) {
      Text("Actualizar")
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
