package com.copitoB.mypos.application.transactions

import com.copitoB.mypos.data.repository.ExpenseRepository
import com.copitoB.mypos.data.repository.SaleRepository

class ReverseExpense(
  private val saleRepository: SaleRepository,
  private val expenseRepository: ExpenseRepository
) {
  data class Result(val transactionId: Long)

  suspend fun execute(transactionId: Long): Result {
    val transaction = saleRepository.findById(transactionId)
      ?: throw IllegalArgumentException("Expense transaction not found: $transactionId")

    require(transaction.type == "EXPENSE") { "Transaction is not an expense" }

    // Reverse accumulator: decrease total_expenses, increase net_balance
    // This is handled by UpdateAccumulator with negative deltas
    saleRepository.softDelete(transactionId)

    return Result(transactionId = transactionId)
  }
}
