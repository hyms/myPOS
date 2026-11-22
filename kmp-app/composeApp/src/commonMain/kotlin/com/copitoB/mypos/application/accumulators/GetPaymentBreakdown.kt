package com.copitoB.mypos.application.accumulators

import com.copitoB.mypos.domain.repository.AccumulatorRepository
import com.copitoB.mypos.domain.valueobjects.Money
import com.copitoB.mypos.domain.valueobjects.PaymentMethod

class GetPaymentBreakdown(
  private val accumulatorRepository: AccumulatorRepository
) {
  operator suspend fun invoke(from: String?, to: String?): Map<PaymentMethod, Money> =
    accumulatorRepository.getSalesByPaymentMethod(from, to)
}