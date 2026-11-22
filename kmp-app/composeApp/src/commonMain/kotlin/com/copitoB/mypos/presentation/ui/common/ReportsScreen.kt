package com.copitoB.mypos.presentation.ui.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.copitoB.mypos.domain.entities.AnnualBreakdown
import com.copitoB.mypos.domain.entities.FinancialSummary
import com.copitoB.mypos.domain.entities.MonthlyBreakdown
import com.copitoB.mypos.domain.entities.PeriodSummary
import com.copitoB.mypos.domain.valueobjects.Money
import com.copitoB.mypos.domain.valueobjects.PaymentMethod
import com.copitoB.mypos.generated.resources.Res
import com.copitoB.mypos.generated.resources.action_refresh
import com.copitoB.mypos.generated.resources.reports_annual_header
import com.copitoB.mypos.generated.resources.reports_monthly_header
import com.copitoB.mypos.generated.resources.reports_payment_header
import com.copitoB.mypos.generated.resources.reports_range_apply
import com.copitoB.mypos.generated.resources.reports_range_custom
import com.copitoB.mypos.generated.resources.reports_range_error
import com.copitoB.mypos.generated.resources.reports_range_month
import com.copitoB.mypos.generated.resources.reports_range_today
import com.copitoB.mypos.generated.resources.reports_stat_expenses
import com.copitoB.mypos.generated.resources.reports_stat_net
import com.copitoB.mypos.generated.resources.reports_stat_purchases
import com.copitoB.mypos.generated.resources.reports_stat_sales
import com.copitoB.mypos.generated.resources.screen_reports
import org.jetbrains.compose.resources.stringResource

enum class ReportRange { TODAY, MONTH, CUSTOM }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportsScreen(
  summary: FinancialSummary?,
  periodSummary: PeriodSummary?,
  payment: Map<PaymentMethod, Money>,
  monthly: List<MonthlyBreakdown>,
  annual: List<AnnualBreakdown>,
  range: ReportRange,
  customFrom: String?,
  customTo: String?,
  customError: Boolean,
  onRangeChange: (ReportRange) -> Unit,
  onApplyCustom: (String, String) -> Unit,
  onRefresh: () -> Unit
) {
  var fromText by remember(customFrom) { mutableStateOf(customFrom ?: "") }
  var toText by remember(customTo) { mutableStateOf(customTo ?: "") }

  Scaffold(
    topBar = {
      TopAppBar(
        title = { Text(stringResource(Res.string.screen_reports)) },
        actions = {
          TextButton(onClick = onRefresh) { Text(stringResource(Res.string.action_refresh)) }
        }
      )
    }
  ) { innerPadding ->
    Column(
      modifier = Modifier.fillMaxSize().padding(innerPadding).verticalScroll(rememberScrollState())
    ) {
      Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        listOf(
          ReportRange.TODAY to Res.string.reports_range_today,
          ReportRange.MONTH to Res.string.reports_range_month,
          ReportRange.CUSTOM to Res.string.reports_range_custom
        ).forEach { (value, label) ->
          FilterChip(
            selected = range == value,
            onClick = { onRangeChange(value) },
            label = { Text(stringResource(label)) }
          )
        }
      }

      if (range == ReportRange.CUSTOM) {
        OutlinedTextField(
          value = fromText,
          onValueChange = { fromText = it },
          label = { Text(stringResource(Res.string.reports_range_custom)) },
          placeholder = { Text("AAAA-MM-DD") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
        )
        OutlinedTextField(
          value = toText,
          onValueChange = { toText = it },
          label = { Text(stringResource(Res.string.reports_range_custom)) },
          placeholder = { Text("AAAA-MM-DD") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
        )
        if (customError) {
          Text(
            text = stringResource(Res.string.reports_range_error),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.error,
            modifier = Modifier.padding(horizontal = 16.dp)
          )
        }
        TextButton(
          onClick = { onApplyCustom(fromText, toText) },
          modifier = Modifier.padding(horizontal = 16.dp)
        ) {
          Text(stringResource(Res.string.reports_range_apply))
        }
      }

      StatCardRow(stringResource(Res.string.reports_stat_sales), summary?.totalSales ?: Money.zero())
      StatCardRow(stringResource(Res.string.reports_stat_purchases), summary?.totalPurchases ?: Money.zero())
      StatCardRow(stringResource(Res.string.reports_stat_expenses), summary?.totalExpenses ?: Money.zero())
      StatCardRow(
        stringResource(Res.string.reports_stat_net),
        summary?.netBalance ?: Money.zero(),
        negative = summary?.netBalanceIsNegative == true
      )

      if (periodSummary != null) {
        StatCardRow(stringResource(Res.string.reports_stat_sales), periodSummary.sales)
        StatCardRow(stringResource(Res.string.reports_stat_purchases), periodSummary.purchases)
        StatCardRow(stringResource(Res.string.reports_stat_expenses), periodSummary.expenses)
        StatCardRow(stringResource(Res.string.reports_stat_net), periodSummary.net)
      }

      Text(
        text = stringResource(Res.string.reports_payment_header),
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
      )
      val paymentTotal = payment.values.sumOf { it.amount.toDouble() }
      PaymentMethod.entries.forEach { method ->
        val amount = payment[method] ?: Money.zero()
        val share = if (paymentTotal > 0.0) (amount.amount.toDouble() / paymentTotal).toFloat() else 0f
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
          ) {
            Text(text = paymentMethodLabel(method), style = MaterialTheme.typography.bodyMedium)
            Text(
              text = amount.amount.setScale(2, java.math.RoundingMode.HALF_UP).toString(),
              style = MaterialTheme.typography.bodyMedium
            )
          }
          LinearProgressIndicator(progress = { share }, modifier = Modifier.fillMaxWidth().padding(top = 2.dp))
        }
      }

      if (monthly.isNotEmpty()) {
        Text(
          text = stringResource(Res.string.reports_monthly_header),
          style = MaterialTheme.typography.titleMedium,
          modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
        monthly.forEach { row ->
          BreakdownCard(
            label = row.month,
            sales = row.sales,
            purchases = row.purchases,
            expenses = row.expenses,
            net = row.net
          )
        }
      }

      if (annual.isNotEmpty()) {
        Text(
          text = stringResource(Res.string.reports_annual_header),
          style = MaterialTheme.typography.titleMedium,
          modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
        annual.forEach { row ->
          BreakdownCard(
            label = row.year,
            sales = row.sales,
            purchases = row.purchases,
            expenses = row.expenses,
            net = row.net
          )
        }
      }
    }
  }
}

@Composable
private fun StatCardRow(label: String, amount: Money, negative: Boolean = false) {
  Row(
    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
    horizontalArrangement = Arrangement.SpaceBetween
  ) {
    Text(text = label, style = MaterialTheme.typography.bodyLarge)
    Text(
      text = if (negative) "-${amount.amount.setScale(2, java.math.RoundingMode.HALF_UP)}" else amount.amount.setScale(2, java.math.RoundingMode.HALF_UP).toString(),
      style = MaterialTheme.typography.bodyLarge,
      color = if (negative) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
    )
  }
}

@Composable
private fun BreakdownCard(
  label: String,
  sales: Money,
  purchases: Money,
  expenses: Money,
  net: Money
) {
  Card(
    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
  ) {
    Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
      Text(text = label, style = MaterialTheme.typography.titleSmall)
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
      ) {
        Text(
          text = "${stringResource(Res.string.reports_stat_sales)}: ${sales.amount.setScale(2, java.math.RoundingMode.HALF_UP)}",
          style = MaterialTheme.typography.bodySmall
        )
        Text(
          text = "${stringResource(Res.string.reports_stat_purchases)}: ${purchases.amount.setScale(2, java.math.RoundingMode.HALF_UP)}",
          style = MaterialTheme.typography.bodySmall
        )
      }
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
      ) {
        Text(
          text = "${stringResource(Res.string.reports_stat_expenses)}: ${expenses.amount.setScale(2, java.math.RoundingMode.HALF_UP)}",
          style = MaterialTheme.typography.bodySmall
        )
        Text(
          text = "${stringResource(Res.string.reports_stat_net)}: ${net.amount.setScale(2, java.math.RoundingMode.HALF_UP)}",
          style = MaterialTheme.typography.bodySmall
        )
      }
    }
  }
}