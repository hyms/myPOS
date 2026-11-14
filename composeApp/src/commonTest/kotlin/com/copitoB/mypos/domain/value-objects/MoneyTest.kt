package com.copitoB.mypos.domain.value-objects

import kotlin.math.BigDecimal
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class MoneyTest {
  @Test
  fun `creates money with valid amount and currency`() {
    val money = Money(BigDecimal("10.50"), Currency.Bs)
    assertEquals(BigDecimal("10.50"), money.amount)
    assertEquals(Currency.Bs, money.currency)
  }

  @Test
  fun `rejects negative amount`() {
    assertFailsWith<IllegalArgumentException> {
      Money(BigDecimal("-1.00"), Currency.Bs)
    }
  }

  @Test
  fun `rejects more than 2 decimal places`() {
    assertFailsWith<IllegalArgumentException> {
      Money(BigDecimal("10.555"), Currency.Bs)
    }
  }

  @Test
  fun `adds same currency money`() {
    val a = Money(BigDecimal("10.50"), Currency.Bs)
    val b = Money(BigDecimal("5.25"), Currency.Bs)
    val result = a.plus(b)
    assertEquals(BigDecimal("15.75"), result.amount)
    assertEquals(Currency.Bs, result.currency)
  }

  @Test
  fun `rejects adding different currencies`() {
    val a = Money(BigDecimal("10.00"), Currency.Bs)
    val b = Money(BigDecimal("5.00"), Currency.Dollar)
    assertFailsWith<IllegalArgumentException> { a.plus(b) }
  }

  @Test
  fun `subtracts same currency money`() {
    val a = Money(BigDecimal("10.50"), Currency.Bs)
    val b = Money(BigDecimal("5.25"), Currency.Bs)
    val result = a.minus(b)
    assertEquals(BigDecimal("5.25"), result.amount)
  }

  @Test
  fun `rejects subtraction resulting in negative`() {
    val a = Money(BigDecimal("5.00"), Currency.Bs)
    val b = Money(BigDecimal("10.00"), Currency.Bs)
    assertFailsWith<IllegalArgumentException> { a.minus(b) }
  }

  @Test
  fun `rounds to 2 decimal places`() {
    val money = Money(BigDecimal("10.555"), Currency.Bs)
    val rounded = money.round2()
    assertEquals(BigDecimal("10.56"), rounded.amount)
  }

  @Test
  fun `detects zero money`() {
    assertTrue(Money(BigDecimal("0.00"), Currency.Bs).isZero())
    assertTrue(!Money(BigDecimal("0.01"), Currency.Bs).isZero())
  }

  @Test
  fun `formats money`() {
    val money = Money(BigDecimal("10.50"), Currency.Bs)
    val formatted = money.toString()
    assertTrue(formatted.contains("10.50"))
  }
}
