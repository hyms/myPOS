package com.copitoB.mypos.application.expenses

import com.copitoB.mypos.application.accumulators.UpdateAccumulator
import com.copitoB.mypos.data.repository.ExpenseRepository
import com.copitoB.mypos.domain.rules.ExpenseRules
import com.copitoB.mypos.domain.value-objects.Currency
import com.copitoB.mypos.domain.value-objects.Money
import java.math.BigDecimal

class RegisterExpense(
  private val expenseRepository: ExpenseRepository,
  private val updateAccumulator: UpdateAccumulator
) {
  data class Result(val expenseId: Long)

  suspend fun execute(
    categoryId: Long,
    amount: String,
    description: String,
    paymentMethod: String
  ): Result {
    val amountBigDecimal = ExpenseRules.validateExpenseAmount(BigDecimal(amount))
    val validatedDescription = ExpenseRules.validateExpenseDescription(description)
    val roundedAmount = ExpenseRules.roundAmount(amountBigDecimal)

    val expenseId = expenseRepository.create(
      ExpenseInput(
        categoryId = categoryId,
        amount = roundedAmount.toString(),
        description = validatedDescription,
        paymentMethod = paymentMethod
      )
    ).id!!

    // Update accumulator atomically
    updateAccumulator(
      UpdateAccumulator.Deltas(
        salesDelta = Money(BigDecimal("0.00"), Currency.Bs),
        purchasesDelta = Money(BigDecimal("0.00"), Currency.Bs),
        expensesDelta = Money(roundedAmount, Currency.Bs)
      )
    )

    return Result(expenseId = expenseId)
  }
}
