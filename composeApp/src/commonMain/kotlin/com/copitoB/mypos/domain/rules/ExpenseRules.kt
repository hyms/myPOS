package com.copitoB.mypos.domain.rules

import com.copitoB.mypos.domain.value-objects.Money
import java.math.BigDecimal

object ExpenseRules {
  fun validateExpenseAmount(amount: BigDecimal): BigDecimal {
    require(amount.signum() > 0) { "Amount must be greater than 0" }
    require(amount.isFinite) { "Amount must be finite" }
    return amount
  }

  fun validateExpenseDescription(description: String): String {
    require(description.trim().isNotEmpty()) { "Description cannot be empty" }
    return description.trim()
  }

  fun roundAmount(amount: BigDecimal): BigDecimal {
    return amount.setScale(2, java.math.RoundingMode.HALF_UP)
  }
}
