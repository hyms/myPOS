package com.copitoB.mypos.domain.rules

import com.copitoB.mypos.domain.value-objects.Money
import com.copitoB.mypos.domain.value-objects.Stock

object StockRules {
  fun validateNonNegative(stock: Stock): Stock {
    require(!stock.isZero() || stock.value.signum() >= 0) { "Stock must be non-negative" }
    return stock
  }

  fun isBelowMin(current: Stock, min: Stock): Boolean = current.isBelow(min)

  fun validateStockAvailable(current: Stock, required: Stock): Stock {
    require(current.value.compareTo(required.value) >= 0) {
      "Insufficient stock: available=${current.value}, required=${required.value}"
    }
    return current
  }
}

object MoneyRules {
  fun validateNonNegative(money: Money): Money {
    require(!money.isZero() || money.amount.signum() >= 0) { "Money must be non-negative" }
    return money
  }

  fun validateSameCurrency(a: Money, b: Money) {
    require(a.currency == b.currency) { "Cannot operate on different currencies" }
  }
}
