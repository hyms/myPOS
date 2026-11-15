package com.copitoB.mypos.domain.rules

import com.copitoB.mypos.domain.valueobjects.Money

object MoneyRules {
  fun validateNonNegative(money: Money): Money {
    require(!money.isZero() || money.amount.signum() >= 0) { "Money must be non-negative" }
    return money
  }

  fun validateSameCurrency(a: Money, b: Money) {
    require(a.currency == b.currency) { "Cannot operate on different currencies" }
  }
}
