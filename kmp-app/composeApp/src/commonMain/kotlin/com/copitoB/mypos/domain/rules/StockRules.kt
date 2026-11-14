package com.copitoB.mypos.domain.rules

import com.copitoB.mypos.domain.entities.Product
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
