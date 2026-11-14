package com.copitoB.mypos.domain.value-objects

import java.math.BigDecimal
import java.math.RoundingMode

data class Money(val amount: BigDecimal, val currency: Currency) {
  init {
    require(amount.scale() <= 2) { "Money amount must have at most 2 decimal places" }
    require(amount.signum() >= 0) { "Money amount must be non-negative" }
  }

  fun plus(other: Money): Money {
    require(this.currency == other.currency) { "Cannot add different currencies" }
    return Money(this.amount.add(other.amount), this.currency)
  }

  fun minus(other: Money): Money {
    require(this.currency == other.currency) { "Cannot subtract different currencies" }
    require(this.amount >= other.amount) { "Result would be negative" }
    return Money(this.amount.subtract(other.amount), this.currency)
  }

  fun round2(): Money = Money(amount.setScale(2, RoundingMode.HALF_UP), currency)

  fun isZero(): Boolean = amount.signum() == 0

  fun isGreaterThan(other: Money): Boolean {
    require(this.currency == other.currency) { "Cannot compare different currencies" }
    return amount.compareTo(other.amount) > 0
  }

  override fun toString(): String = currency.format(amount)
}
