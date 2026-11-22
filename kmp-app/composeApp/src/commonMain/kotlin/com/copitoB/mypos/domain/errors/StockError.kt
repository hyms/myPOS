package com.copitoB.mypos.domain.errors

import com.copitoB.mypos.domain.valueobjects.Stock

sealed class StockError(message: String) : Exception(message) {
  class Insufficient(val available: Stock, val required: Stock) :
    StockError("Insufficient stock: available=${available.value}, required=${required.value}")

  class Invalid(val detail: String) :
    StockError("Invalid stock value: $detail")
}