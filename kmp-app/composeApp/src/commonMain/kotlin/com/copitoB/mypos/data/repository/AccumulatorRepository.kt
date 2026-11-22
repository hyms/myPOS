package com.copitoB.mypos.data.repository

import com.copitoB.mypos.domain.entities.AnnualBreakdown
import com.copitoB.mypos.domain.entities.FinancialSummary
import com.copitoB.mypos.domain.entities.MonthlyBreakdown
import com.copitoB.mypos.domain.entities.PeriodSummary
import com.copitoB.mypos.domain.valueobjects.Currency
import com.copitoB.mypos.domain.valueobjects.Money
import com.copitoB.mypos.domain.valueobjects.PaymentMethod
import com.copitoB.mypos.domain.valueobjects.toMoneyRounded
import app.cash.sqldelight.db.SqlDriver
import com.copitoB.mypos.data.repository.TpvDatabase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.math.RoundingMode

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
      netBalance = Money(java.math.BigDecimal.valueOf(netAbs).setScale(2, RoundingMode.HALF_UP), Currency.Bs),
      netBalanceIsNegative = rawNet < 0.0,
      updatedAt = row.updated_at
    )
  }

  override suspend fun getPeriodSummary(period: String): PeriodSummary {
    val sales = queries.financial_accumulatorsQueries.salesSumByTypeMonth(type = "SALE", period = period).executeAsOne().SUM
    val purchases = queries.financial_accumulatorsQueries.salesSumByTypeMonth(type = "PURCHASE", period = period).executeAsOne().SUM
    val expenses = queries.financial_accumulatorsQueries.salesSumByTypeMonth(type = "EXPENSE", period = period).executeAsOne().SUM

    val salesMoney = sales.toMoney()
    val purchasesMoney = purchases.toMoney()
    val expensesMoney = expenses.toMoney()
    val net = salesMoney.minus(purchasesMoney).minus(expensesMoney)

    return PeriodSummary(
      period = period,
      sales = salesMoney,
      purchases = purchasesMoney,
      expenses = expensesMoney,
      net = net
    )
  }

  override suspend fun getTotalsInRange(from: String?, to: String?): PeriodSummary {
    fun sum(type: String): Double =
      queries.financial_accumulatorsQueries.salesSumByTypeRange(type = type, from = from, to = to)
        .executeAsOneOrNull()?.SUM ?: 0.0

    val salesMoney = sum("SALE").toMoney()
    val purchasesMoney = sum("PURCHASE").toMoney()
    val expensesMoney = sum("EXPENSE").toMoney()

    return PeriodSummary(
      period = "",
      sales = salesMoney,
      purchases = purchasesMoney,
      expenses = expensesMoney,
      net = salesMoney.minus(purchasesMoney).minus(expensesMoney)
    )
  }

  override suspend fun getSalesByPaymentMethod(from: String?, to: String?): Map<PaymentMethod, Money> {
    val rows = queries.financial_accumulatorsQueries.salesSumByPaymentMethod(from = from, to = to)
      .executeAsList()
      .associate { row -> runCatching { PaymentMethod.valueOf(row.method) }.getOrNull() to row.total }
      .filterKeys { it != null }
    return PaymentMethod.entries.associateWith { method ->
      (rows[method] ?: 0.0).toMoney()
    }
  }

  override suspend fun getMonthlyBreakdown(from: String?, to: String?): List<MonthlyBreakdown> {
    return queries.financial_accumulatorsQueries.salesSumByTypeMonthRange(from = from, to = to)
      .executeAsList()
      .map { row ->
        val sales = row.sales.toMoney()
        val purchases = row.purchases.toMoney()
        val expenses = row.expenses.toMoney()
        MonthlyBreakdown(
          month = row.month,
          sales = sales,
          purchases = purchases,
          expenses = expenses,
          net = sales.minus(purchases).minus(expenses)
        )
      }
  }

  override suspend fun getAnnualBreakdown(from: String?, to: String?): List<AnnualBreakdown> {
    return queries.financial_accumulatorsQueries.salesSumByTypeYearRange(from = from, to = to)
      .executeAsList()
      .map { row ->
        val sales = row.sales.toMoney()
        val purchases = row.purchases.toMoney()
        val expenses = row.expenses.toMoney()
        AnnualBreakdown(
          year = row.year,
          sales = sales,
          purchases = purchases,
          expenses = expenses,
          net = sales.minus(purchases).minus(expenses)
        )
      }
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

  override suspend fun revertAccumulator(salesAmount: Money, purchasesAmount: Money, expensesAmount: Money) {
    queries.financial_accumulatorsQueries.transaction(true) {
      queries.financial_accumulatorsQueries.financialAccumulatorsInsertDefault(
        updatedAt = java.time.Instant.now().toString()
      )
      val current = queries.financial_accumulatorsQueries.financialAccumulatorsFindById(id = 1L).executeAsOne()
      queries.financial_accumulatorsQueries.financialAccumulatorsUpdate(
        id = 1L,
        totalSales = current.total_sales - salesAmount.amount.toDouble(),
        totalPurchases = current.total_purchases - purchasesAmount.amount.toDouble(),
        totalExpenses = current.total_expenses - expensesAmount.amount.toDouble(),
        netBalance = current.net_balance - salesAmount.amount.toDouble() + purchasesAmount.amount.toDouble() + expensesAmount.amount.toDouble(),
        updatedAt = java.time.Instant.now().toString()
      )
    }
  }

  override fun watchCashSummary(): Flow<FinancialSummary> = flow {
    emit(getCashSummary())
  }

  private fun Double?.toMoney(): Money = Money(
    java.math.BigDecimal.valueOf(this ?: 0.0).setScale(2, RoundingMode.HALF_UP),
    Currency.Bs
  )
}