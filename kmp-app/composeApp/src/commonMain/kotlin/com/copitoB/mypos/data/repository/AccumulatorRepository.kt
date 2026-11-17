package com.copitoB.mypos.data.repository

import com.copitoB.mypos.domain.entities.FinancialSummary
import com.copitoB.mypos.domain.entities.PeriodSummary
import com.copitoB.mypos.domain.valueobjects.Currency
import com.copitoB.mypos.domain.valueobjects.Money
import com.copitoB.mypos.domain.valueobjects.toMoneyRounded
import app.cash.sqldelight.db.SqlDriver
import com.copitoB.mypos.data.repository.TpvDatabase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class SqliteAccumulatorRepository(
  private val driver: SqlDriver
) : com.copitoB.mypos.domain.repository.AccumulatorRepository {
  private val queries = TpvDatabase(driver)

  override suspend fun getCashSummary(): FinancialSummary {
    queries.financial_accumulatorsQueries.financialAccumulatorsInsertDefault(
      updatedAt = java.time.Instant.now().toString()
    )
    val row = queries.financial_accumulatorsQueries.financialAccumulatorsFindById(id = 1L).executeAsOne()
    val rawNet = row.net_balance
    val netAbs = if (rawNet < 0.0) -rawNet else rawNet
    return FinancialSummary(
      totalSales = row.total_sales.toMoneyRounded(Currency.Bs),
      totalPurchases = row.total_purchases.toMoneyRounded(Currency.Bs),
      totalExpenses = row.total_expenses.toMoneyRounded(Currency.Bs),
      netBalance = Money(java.math.BigDecimal.valueOf(netAbs).setScale(2, java.math.RoundingMode.HALF_UP), Currency.Bs),
      netBalanceIsNegative = rawNet < 0.0,
      updatedAt = row.updated_at
    )
  }

  override suspend fun getPeriodSummary(period: String): PeriodSummary {
    val sales = queries.financial_accumulatorsQueries.salesSumByTypeMonth(type = "SALE", period = period).executeAsOne().SUM
    val purchases = queries.financial_accumulatorsQueries.salesSumByTypeMonth(type = "PURCHASE", period = period).executeAsOne().SUM
    val expenses = queries.financial_accumulatorsQueries.salesSumByTypeMonth(type = "EXPENSE", period = period).executeAsOne().SUM

    val salesMoney = Money(java.math.BigDecimal.valueOf(sales ?: 0.0).setScale(2, java.math.RoundingMode.HALF_UP), Currency.Bs)
    val purchasesMoney = Money(java.math.BigDecimal.valueOf(purchases ?: 0.0).setScale(2, java.math.RoundingMode.HALF_UP), Currency.Bs)
    val expensesMoney = Money(java.math.BigDecimal.valueOf(expenses ?: 0.0).setScale(2, java.math.RoundingMode.HALF_UP), Currency.Bs)
    val net = salesMoney.minus(purchasesMoney).minus(expensesMoney)

    return PeriodSummary(
      period = period,
      sales = salesMoney,
      purchases = purchasesMoney,
      expenses = expensesMoney,
      net = net
    )
  }

  override suspend fun updateAccumulator(salesDelta: Money, purchasesDelta: Money, expensesDelta: Money) {
    queries.financial_accumulatorsQueries.transaction(true) {
      queries.financial_accumulatorsQueries.financialAccumulatorsInsertDefault(
        updatedAt = java.time.Instant.now().toString()
      )
      val current = queries.financial_accumulatorsQueries.financialAccumulatorsFindById(id = 1L).executeAsOne()
      queries.financial_accumulatorsQueries.financialAccumulatorsUpdate(
        id = 1L,
        totalSales = current.total_sales + salesDelta.amount.toDouble(),
        totalPurchases = current.total_purchases + purchasesDelta.amount.toDouble(),
        totalExpenses = current.total_expenses + expensesDelta.amount.toDouble(),
        netBalance = current.net_balance + salesDelta.amount.toDouble() - purchasesDelta.amount.toDouble() - expensesDelta.amount.toDouble(),
        updatedAt = java.time.Instant.now().toString()
      )
    }
  }

  override fun watchCashSummary(): Flow<FinancialSummary> = flow {
    emit(getCashSummary())
  }
}