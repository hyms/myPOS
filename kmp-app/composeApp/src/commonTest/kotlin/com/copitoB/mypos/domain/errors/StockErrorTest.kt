package com.copitoB.mypos.domain.errors

import com.copitoB.mypos.domain.rules.StockRules
import com.copitoB.mypos.domain.valueobjects.Stock
import java.math.BigDecimal
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class StockErrorTest {

  @Test
  fun `validateStockAvailable throws Insufficient when stock too low`() {
    val error = assertFailsWith<StockError.Insufficient> {
      StockRules.validateStockAvailable(
        current = Stock(BigDecimal("2")),
        required = Stock(BigDecimal("5"))
      )
    }
    assertEquals(BigDecimal("2"), error.available.value)
    assertEquals(BigDecimal("5"), error.required.value)
  }

  @Test
  fun `validateStockAvailable returns stock when sufficient`() {
    val current = Stock(BigDecimal("10"))
    val result = StockRules.validateStockAvailable(current, Stock(BigDecimal("5")))
    assertEquals(current, result)
  }
}