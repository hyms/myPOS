package com.copitoB.mypos.data.repository

import com.copitoB.mypos.domain.entities.FinancialSummary
import com.copitoB.mypos.domain.entities.PeriodSummary
import com.copitoB.mypos.domain.value-objects.Money
import app.cash.sqldelight.CoroutinesExtensions.awaitFirstOrNull
import app.cash.sqldelight.CoroutinesExtensions.asFlow
import app.cash.sqldelight.db
import com.copitoB.mypos.data.repository.TpvDatabase.Companion.Driver
import kotlinx.coroutines.flow.flow

interface AccumulatorRepository {
  fun getCashSummary(): FinancialSummary
  fun getPeriodSummary(period: String): PeriodSummary
  fun updateAccumulator(salesDelta: Money, purchasesDelta: Money, expensesDelta: Money)
  fun watchCashSummary(): Flow<FinancialSummary>
}

class SqliteAccumulatorRepository(
  private val driver: Driver
) : AccumulatorRepository {
  private val queries = com.copitoB.mypos.data.repository.TpvDatabase(driver)

  override fun getCashSummary(): FinancialSummary {
    val row = queries.FinancialAccumulatorsQueries.findById(id = 1L).awaitFirstOrNull()
      ?: throw IllegalStateException("Accumulator not initialized")
    return FinancialSummary(
      totalSales = Money(BigDecimal(row.totalSales.toString()), com.copitoB.mypos.domain.value-objects.Currency.Bs),
      totalPurchases = Money(BigDecimal(row.totalPurchases.toString()), com.copitoB.mypos.domain.value-objects.Currency.Bs),
      totalExpenses = Money(BigDecimal(row.totalExpenses.toString()), com.copitoB.mypos.domain.value-objects.Currency.Bs),
      netBalance = Money(BigDecimal(row.netBalance.toString()), com.copitoB.mypos.domain.value-objects.Currency.Bs),
      updatedAt = row.updatedAt
    )
  }

  override fun getPeriodSummary(period: String): PeriodSummary {
    // Aggregate from sales_transactions for the given period (MONTH or YEAR)
    val sales = queries.SalesTransactionsQueries.sumByType(type = "SALE", period = period)
    val purchases = queries.SalesTransactionsQueries.sumByType(type = "PURCHASE", period = period)
    val expenses = queries.SalesTransactionsQueries.sumByType(type = "EXPENSE", period = period)

    val salesMoney = Money(BigDecimal(sales.toString()), com.copitoB.mypos.domain.value-objects.Currency.Bs)
    val purchasesMoney = Money(BigDecimal(purchases.toString()), com.copitoB.mypos.domain.value-objects.Currency.Bs)
    val expensesMoney = Money(BigDecimal(expenses.toString()), com.copitoB.mypos.domain.value-objects.Currency.Bs)
    val net = salesMoney.minus(purchasesMoney).minus(expensesMoney)

    return PeriodSummary(
      period = period,
      sales = salesMoney,
      purchases = purchasesMoney,
      expenses = expensesMoney,
      net = net
    )
  }

  override fun updateAccumulator(salesDelta: Money, purchasesDelta: Money, expensesDelta: Money) {
    queries.db.withTransaction {
      queries.FinancialAccumulatorsQueries.update(
        id = 1L,
        totalSales = queries.FinancialAccumulatorsQueries.findById(id = 1L).awaitFirstOrNull()!!.totalSales + salesDelta.amount.toDouble(),
        totalPurchases = queries.FinancialAccumulatorsQueries.findById(id = 1L).awaitFirstOrNull()!!.totalPurchases + purchasesDelta.amount.toDouble(),
        totalExpenses = queries.FinancialAccumulatorsQueries.findById(id = 1L).awaitFirstOrNull()!!.totalExpenses + expensesDelta.amount.toDouble(),
        netBalance = queries.FinancialAccumulatorsQueries.findById(id = 1L).awaitFirstOrNull()!!.netBalance + salesDelta.amount.toDouble() - purchasesDelta.amount.toDouble() - expensesDelta.amount.toDouble(),
        updatedAt = java.time.Instant.now().toString()
      )
    }
  }

  override fun watchCashSummary(): Flow<FinancialSummary> = flow {
    emit(getCashSummary())
  }
}
