package com.copitoB.mypos.application.accumulators

import com.copitoB.mypos.domain.entities.FinancialSummary
import com.copitoB.mypos.domain.entities.PeriodSummary
import com.copitoB.mypos.domain.repository.AccumulatorRepository
import com.copitoB.mypos.domain.valueobjects.Money
import java.math.BigDecimal

class GetCashSummary(
  private val accumulatorRepository: AccumulatorRepository
) {
  operator suspend fun invoke(): FinancialSummary {
    return accumulatorRepository.getCashSummary()
  }
}

class GetPeriodSummary(
  private val accumulatorRepository: AccumulatorRepository
) {
  operator suspend fun invoke(period: String): PeriodSummary {
    return accumulatorRepository.getPeriodSummary(period)
  }
}

class UpdateAccumulator(
  private val accumulatorRepository: AccumulatorRepository
) {
  data class Deltas(
    val salesDelta: Money,
    val purchasesDelta: Money,
    val expensesDelta: Money
  )

  operator suspend fun invoke(deltas: Deltas) {
    accumulatorRepository.updateAccumulator(
      salesDelta = deltas.salesDelta,
      purchasesDelta = deltas.purchasesDelta,
      expensesDelta = deltas.expensesDelta
    )
  }

  suspend fun revert(deltas: Deltas) {
    accumulatorRepository.revertAccumulator(
      salesAmount = deltas.salesDelta,
      purchasesAmount = deltas.purchasesDelta,
      expensesAmount = deltas.expensesDelta
    )
  }
}
