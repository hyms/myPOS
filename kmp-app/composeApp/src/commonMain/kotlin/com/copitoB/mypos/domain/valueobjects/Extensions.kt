package com.copitoB.mypos.domain.valueobjects

import java.math.BigDecimal
import java.math.RoundingMode

fun String.toMoney(currency: Currency = Currency.Bs): Money {
  return Money(BigDecimal(this), currency)
}

fun String.toStock(): Stock {
  return Stock(BigDecimal(this))
}

fun Stock?.toStringOrNull(): String? = this?.value?.toString()

fun Money?.toStringOrNull(): String? = this?.amount?.toString()

// SQLite REAL sums can produce long decimal tails (e.g. 0.30000000000000004).
// Money/Stock require bounded scales, so round at the mapping boundary.
fun Double.toMoneyRounded(currency: Currency = Currency.Bs): Money {
  return Money(BigDecimal.valueOf(this).setScale(2, RoundingMode.HALF_UP), currency)
}

fun Double.toStockRounded(): Stock {
  return Stock(BigDecimal.valueOf(this).setScale(3, RoundingMode.HALF_UP))
}
