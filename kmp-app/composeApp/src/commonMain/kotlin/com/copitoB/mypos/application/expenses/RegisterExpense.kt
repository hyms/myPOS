package com.copitoB.mypos.application.expenses

import com.copitoB.mypos.application.accumulators.UpdateAccumulator
import com.copitoB.mypos.domain.entities.ExpenseInput
import com.copitoB.mypos.domain.repository.ExpenseRepository
import com.copitoB.mypos.domain.rules.ExpenseRules
import com.copitoB.mypos.domain.valueobjects.Money

class RegisterExpense(
  private val expenseRepository: ExpenseRepository,
  private val updateAccumulator: UpdateAccumulator
) {
  data class Result(val expenseId: Long)

  suspend fun execute(
    categoryId: Long,
    amount: Money,
    description: String,
    paymentMethod: com.copitoB.mypos.domain.valueobjects.PaymentMethod
  ): Result {
    val validatedAmount = ExpenseRules.validateExpenseAmount(amount)
    val roundedAmount = ExpenseRules.roundAmount(validatedAmount)
    val validatedDescription = ExpenseRules.validateExpenseDescription(description)

    val expenseId = expenseRepository.create(
      ExpenseInput(
        categoryId = categoryId,
        amount = roundedAmount,
        description = validatedDescription,
        paymentMethod = paymentMethod
      )
    ).id!!

    // Update accumulator atomically
    updateAccumulator(
      UpdateAccumulator.Deltas(
        salesDelta = Money(java.math.BigDecimal("0.00"), amount.currency),
        purchasesDelta = Money(java.math.BigDecimal("0.00"), amount.currency),
        expensesDelta = roundedAmount
      )
    )

    return Result(expenseId = expenseId)
  }
}