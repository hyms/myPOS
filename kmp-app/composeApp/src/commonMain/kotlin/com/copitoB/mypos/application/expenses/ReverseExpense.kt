package com.copitoB.mypos.application.expenses

import com.copitoB.mypos.application.accumulators.UpdateAccumulator
import com.copitoB.mypos.domain.repository.ExpenseRepository
import com.copitoB.mypos.domain.valueobjects.Money

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
        salesDelta = Money(java.math.BigDecimal("0.00"), transaction.amount.currency),
        purchasesDelta = Money(java.math.BigDecimal("0.00"), transaction.amount.currency),
        expensesDelta = Money(transaction.amount.amount.negate(), transaction.amount.currency)
      )
    )

    expenseRepository.softDelete(transactionId)

    return Result(transactionId = transactionId)
  }
}