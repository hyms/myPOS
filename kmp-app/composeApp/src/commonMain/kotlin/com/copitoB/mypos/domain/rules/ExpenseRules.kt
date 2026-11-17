package com.copitoB.mypos.domain.rules

import com.copitoB.mypos.domain.valueobjects.Money

object ExpenseRules {
  fun validateExpenseAmount(amount: Money): Money {
    require(amount.amount.signum() > 0) { "Amount must be greater than 0" }
    return amount
  }

  fun validateExpenseDescription(description: String): String {
    require(description.trim().isNotEmpty()) { "Description cannot be empty" }
    return description.trim()
  }

  fun roundAmount(amount: Money): Money {
    return Money(amount.amount.setScale(2, java.math.RoundingMode.HALF_UP), amount.currency)
  }
}