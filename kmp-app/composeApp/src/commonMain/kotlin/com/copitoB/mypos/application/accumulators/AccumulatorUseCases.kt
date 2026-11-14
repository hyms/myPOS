package com.copitoB.mypos.application.accumulators

import com.copitoB.mypos.domain.entities.FinancialSummary
import com.copitoB.mypos.domain.entities.PeriodSummary
import com.copitoB.mypos.domain.repository.AccumulatorRepository
import com.copitoB.mypos.domain.value-objects.Currency
import com.copitoB.mypos.domain.value-objects.Money
import java.math.BigDecimal

class GetCashSummary(
  private val accumulatorRepository: AccumulatorRepository
) {
  operator fun invoke(): FinancialSummary {
    return accumulatorRepository.getCashSummary()
  }
}

class GetPeriodSummary(
  private val accumulatorRepository: AccumulatorRepository
) {
  operator fun invoke(period: String): PeriodSummary {
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

  operator fun invoke(deltas: Deltas) {
    accumulatorRepository.updateAccumulator(deltas)
  }
}
