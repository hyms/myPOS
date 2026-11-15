package com.copitoB.mypos.domain.repository

import com.copitoB.mypos.domain.entities.FinancialSummary
import com.copitoB.mypos.domain.entities.PeriodSummary
import com.copitoB.mypos.domain.valueobjects.Money
import kotlinx.coroutines.flow.Flow

interface AccumulatorRepository {
  suspend fun getCashSummary(): FinancialSummary
  suspend fun getPeriodSummary(period: String): PeriodSummary
  suspend fun updateAccumulator(salesDelta: Money, purchasesDelta: Money, expensesDelta: Money)
  fun watchCashSummary(): Flow<FinancialSummary>
}
