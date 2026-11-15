package com.copitoB.mypos.application.expenses

import com.copitoB.mypos.application.accumulators.UpdateAccumulator
import com.copitoB.mypos.domain.repository.ExpenseRepository
import com.copitoB.mypos.domain.valueobjects.Currency
import com.copitoB.mypos.domain.valueobjects.Money
import java.math.BigDecimal

class ReverseExpense(
  private val expenseRepository: ExpenseRepository,
  private val updateAccumulator: UpdateAccumulator
) {
  data class Result(val transactionId: Long)

  suspend fun execute(transactionId: Long): Result {
    val transaction = expenseRepository.findById(transactionId)
      ?: throw IllegalArgumentException("Expense transaction not found: $transactionId")

    // Reverse accumulator: decrease total_expenses, increase net_balance
    updateAccumulator(
      UpdateAccumulator.Deltas(
        salesDelta = Money(BigDecimal("0.00"), Currency.Bs),
        purchasesDelta = Money(BigDecimal("0.00"), Currency.Bs),
        expensesDelta = Money(transaction.amount.amount.negate(), Currency.Bs)
      )
    )

    expenseRepository.softDelete(transactionId)

    return Result(transactionId = transactionId)
  }
}
