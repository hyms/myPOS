package com.copitoB.mypos.domain.valueobjects

import java.math.BigDecimal
import kotlin.test.Test
import kotlin.test.assertEquals

class StockDisplayTest {

  @Test
  fun `whole stock drops trailing zeros`() {
    assertEquals("10", Stock(BigDecimal("10.000")).toDisplayString())
    assertEquals("0", Stock(BigDecimal.ZERO).toDisplayString())
  }

  @Test
  fun `fractional stock keeps significant digits`() {
    assertEquals("4.5", Stock(BigDecimal("4.500")).toDisplayString())
    assertEquals("4.125", Stock(BigDecimal("4.125")).toDisplayString())
  }

  @Test
  fun `toString keeps three decimals`() {
    assertEquals("10.000", Stock(BigDecimal("10.000")).toString())
  }
}