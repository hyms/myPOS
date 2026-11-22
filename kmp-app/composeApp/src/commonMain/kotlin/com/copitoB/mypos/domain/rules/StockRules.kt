package com.copitoB.mypos.domain.rules

import com.copitoB.mypos.domain.entities.Product
import com.copitoB.mypos.domain.errors.StockError
import com.copitoB.mypos.domain.valueobjects.Stock

object StockRules {
  fun validateNonNegative(stock: Stock): Stock {
    require(!stock.isZero() || stock.value.signum() >= 0) { "Stock must be non-negative" }
    return stock
  }

  fun isBelowMin(current: Stock, min: Stock): Boolean = current.isBelow(min)

  fun validateStockAvailable(current: Stock, required: Stock): Stock {
    if (current.value.compareTo(required.value) < 0) {
      throw StockError.Insufficient(available = current, required = required)
    }
    return current
  }

  fun checkLowStock(product: Product): LowStockAlert? {
    return if (product.isBelowMinStock()) {
      LowStockAlert(product, product.currentStock, product.minStock)
    } else null
  }
}

data class LowStockAlert(
  val product: Product,
  val currentStock: Stock,
  val minStock: Stock
)
