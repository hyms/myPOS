package com.copitoB.mypos.domain.value-objects

import java.math.BigDecimal

fun String.toMoney(currency: Currency = Currency.Bs): Money {
  return Money(BigDecimal(this), currency)
}

fun String.toStock(): Stock {
  return Stock(BigDecimal(this))
}

fun Stock?.toStringOrNull(): String? = this?.value?.toString()

fun Money?.toStringOrNull(): String? = this?.amount?.toString()
