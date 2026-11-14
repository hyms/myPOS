package com.copitoB.mypos.domain.value-objects

import kotlin.math.BigDecimal
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class StockTest {
  @Test
  fun `creates stock with valid value`() {
    val stock = Stock(BigDecimal("10.500"))
    assertEquals(BigDecimal("10.500"), stock.value)
  }

  @Test
  fun `rejects negative stock`() {
    assertFailsWith<IllegalArgumentException> {
      Stock(BigDecimal("-1.000"))
    }
  }

  @Test
  fun `rejects more than 3 decimal places`() {
    assertFailsWith<IllegalArgumentException> {
      Stock(BigDecimal("10.5005"))
    }
  }

  @Test
  fun `adds stock`() {
    val a = Stock(BigDecimal("10.500"))
    val b = Stock(BigDecimal("5.250"))
    val result = a.plus(b)
    assertEquals(BigDecimal("15.750"), result.value)
  }

  @Test
  fun `subtracts stock`() {
    val a = Stock(BigDecimal("10.500"))
    val b = Stock(BigDecimal("5.250"))
    val result = a.minus(b)
    assertEquals(BigDecimal("5.250"), result.value)
  }

  @Test
  fun `rejects subtraction below zero`() {
    val a = Stock(BigDecimal("5.000"))
    val b = Stock(BigDecimal("10.000"))
    assertFailsWith<IllegalArgumentException> { a.minus(b) }
  }

  @Test
  fun `detects stock below threshold`() {
    val current = Stock(BigDecimal("5.000"))
    val min = Stock(BigDecimal("10.000"))
    assertTrue(current.isBelow(min))
  }

  @Test
  fun `detects zero stock`() {
    assertTrue(Stock(BigDecimal("0.000")).isZero())
  }
}
