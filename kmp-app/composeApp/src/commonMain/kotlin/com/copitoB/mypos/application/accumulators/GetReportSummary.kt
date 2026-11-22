package com.copitoB.mypos.application.accumulators

import com.copitoB.mypos.domain.entities.AnnualBreakdown
import com.copitoB.mypos.domain.entities.MonthlyBreakdown
import com.copitoB.mypos.domain.entities.PeriodSummary
import com.copitoB.mypos.domain.repository.AccumulatorRepository

class GetReportSummary(
  private val accumulatorRepository: AccumulatorRepository
) {
  data class Report(
    val totals: PeriodSummary,
    val monthly: List<MonthlyBreakdown>,
    val annual: List<AnnualBreakdown>
  )

  operator suspend fun invoke(from: String?, to: String?): Report = Report(
    totals = accumulatorRepository.getTotalsInRange(from, to),
    monthly = accumulatorRepository.getMonthlyBreakdown(from, to),
    annual = accumulatorRepository.getAnnualBreakdown(from, to)
  )
}