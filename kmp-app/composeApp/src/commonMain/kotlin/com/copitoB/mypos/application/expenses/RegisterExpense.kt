package com.copitoB.mypos.application.expenses

import com.copitoB.mypos.application.accumulators.UpdateAccumulator
import com.copitoB.mypos.domain.entities.ExpenseInput
import com.copitoB.mypos.domain.repository.ExpenseRepository
import com.copitoB.mypos.domain.rules.ExpenseRules
import com.copitoB.mypos.domain.valueobjects.Currency
import com.copitoB.mypos.domain.valueobjects.Money
import com.copitoB.mypos.domain.valueobjects.PaymentMethod
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
    paymentMethod: PaymentMethod
  ): Result {
    val amountBigDecimal = ExpenseRules.validateExpenseAmount(BigDecimal(amount))
    val validatedDescription = ExpenseRules.validateExpenseDescription(description)
    val roundedAmount = ExpenseRules.roundAmount(amountBigDecimal)

    val expenseId = expenseRepository.create(
      ExpenseInput(
        categoryId = categoryId,
        amount = roundedAmount.toString(),
        description = validatedDescription,
        paymentMethod = paymentMethod.code
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
