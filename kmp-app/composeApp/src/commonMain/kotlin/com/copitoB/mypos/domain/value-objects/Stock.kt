package com.copitoB.mypos.domain.value-objects

import java.math.BigDecimal
import java.math.RoundingMode

data class Stock(val value: BigDecimal) {
  init {
    require(value.scale() <= 3) { "Stock must have at most 3 decimal places" }
    require(value.signum() >= 0) { "Stock must be non-negative" }
  }

  fun plus(other: Stock): Stock = Stock(value.add(other.value))

  fun minus(other: Stock): Stock {
    require(value >= other.value) { "Stock cannot go negative" }
    return Stock(value.subtract(other.value))
  }

  fun isBelow(threshold: Stock): Boolean = value.compareTo(threshold.value) < 0

  fun isZero(): Boolean = value.signum() == 0

  override fun toString(): String = value.setScale(3, RoundingMode.HALF_UP).toString()
}
