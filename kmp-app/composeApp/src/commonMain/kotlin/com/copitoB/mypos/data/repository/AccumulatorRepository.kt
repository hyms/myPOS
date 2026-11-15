package com.copitoB.mypos.data.repository

import com.copitoB.mypos.domain.entities.FinancialSummary
import com.copitoB.mypos.domain.entities.PeriodSummary
import com.copitoB.mypos.domain.valueobjects.Currency
import com.copitoB.mypos.domain.valueobjects.Money
import app.cash.sqldelight.db.SqlDriver
import com.copitoB.mypos.data.repository.TpvDatabase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class SqliteAccumulatorRepository(
  private val driver: SqlDriver
) : com.copitoB.mypos.domain.repository.AccumulatorRepository {
  private val queries = TpvDatabase(driver)

  override suspend fun getCashSummary(): FinancialSummary {
    val row = queries.financial_accumulatorsQueries.financialAccumulatorsFindById(id = 1L).executeAsOne()
    return FinancialSummary(
      totalSales = Money(java.math.BigDecimal(row.total_sales.toString()), Currency.Bs),
      totalPurchases = Money(java.math.BigDecimal(row.total_purchases.toString()), Currency.Bs),
      totalExpenses = Money(java.math.BigDecimal(row.total_expenses.toString()), Currency.Bs),
      netBalance = Money(java.math.BigDecimal(row.net_balance.toString()), Currency.Bs),
      updatedAt = row.updated_at
    )
  }

  override suspend fun getPeriodSummary(period: String): PeriodSummary {
    val sales = queries.financial_accumulatorsQueries.salesSumByTypeMonth(type = "SALE", period = period).executeAsOne().SUM
    val purchases = queries.financial_accumulatorsQueries.salesSumByTypeMonth(type = "PURCHASE", period = period).executeAsOne().SUM
    val expenses = queries.financial_accumulatorsQueries.salesSumByTypeMonth(type = "EXPENSE", period = period).executeAsOne().SUM

    val salesMoney = Money(java.math.BigDecimal((sales ?: 0.0).toString()), Currency.Bs)
    val purchasesMoney = Money(java.math.BigDecimal((purchases ?: 0.0).toString()), Currency.Bs)
    val expensesMoney = Money(java.math.BigDecimal((expenses ?: 0.0).toString()), Currency.Bs)
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
      queries.financial_accumulatorsQueries.financialAccumulatorsUpdate(
        id = 1L,
        totalSales = queries.financial_accumulatorsQueries.financialAccumulatorsFindById(id = 1L).executeAsOne().total_sales + salesDelta.amount.toDouble(),
        totalPurchases = queries.financial_accumulatorsQueries.financialAccumulatorsFindById(id = 1L).executeAsOne().total_purchases + purchasesDelta.amount.toDouble(),
        totalExpenses = queries.financial_accumulatorsQueries.financialAccumulatorsFindById(id = 1L).executeAsOne().total_expenses + expensesDelta.amount.toDouble(),
        netBalance = queries.financial_accumulatorsQueries.financialAccumulatorsFindById(id = 1L).executeAsOne().net_balance + salesDelta.amount.toDouble() - purchasesDelta.amount.toDouble() - expensesDelta.amount.toDouble(),
        updatedAt = java.time.Instant.now().toString()
      )
    }
  }

  override fun watchCashSummary(): Flow<FinancialSummary> = flow {
    emit(getCashSummary())
  }
}