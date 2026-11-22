package com.copitoB.mypos.domain.repository

import com.copitoB.mypos.domain.entities.AnnualBreakdown
import com.copitoB.mypos.domain.entities.FinancialSummary
import com.copitoB.mypos.domain.entities.MonthlyBreakdown
import com.copitoB.mypos.domain.entities.PeriodSummary
import com.copitoB.mypos.domain.valueobjects.Money
import com.copitoB.mypos.domain.valueobjects.PaymentMethod
import kotlinx.coroutines.flow.Flow

interface AccumulatorRepository {
  suspend fun getCashSummary(): FinancialSummary
  suspend fun getPeriodSummary(period: String): PeriodSummary
  suspend fun getTotalsInRange(from: String?, to: String?): PeriodSummary
  suspend fun getSalesByPaymentMethod(from: String?, to: String?): Map<PaymentMethod, Money>
  suspend fun getMonthlyBreakdown(from: String?, to: String?): List<MonthlyBreakdown>
  suspend fun getAnnualBreakdown(from: String?, to: String?): List<AnnualBreakdown>
  suspend fun updateAccumulator(salesDelta: Money, purchasesDelta: Money, expensesDelta: Money)
  suspend fun revertAccumulator(salesAmount: Money, purchasesAmount: Money, expensesAmount: Money)
  fun watchCashSummary(): Flow<FinancialSummary>
}