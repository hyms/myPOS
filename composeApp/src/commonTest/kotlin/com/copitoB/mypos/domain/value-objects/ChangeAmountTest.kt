package com.copitoB.mypos.domain.value-objects

import kotlin.math.BigDecimal
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class ChangeAmountTest {
  @Test
  fun `creates change amount`() {
    val received = Money(BigDecimal("20.00"), Currency.Bs)
    val total = Money(BigDecimal("10.50"), Currency.Bs)
    val change = ChangeAmount(received, total)
    assertEquals(BigDecimal("9.50"), change.change.amount)
    assertTrue(change.isSufficient())
    assertEquals("green", change.colorClass)
  }

  @Test
  fun `detects insufficient funds`() {
    val received = Money(BigDecimal("5.00"), Currency.Bs)
    val total = Money(BigDecimal("10.00"), Currency.Bs)
    val change = ChangeAmount(received, total)
    assertTrue(!change.isSufficient())
    assertEquals("red", change.colorClass)
  }

  @Test
  fun `rejects different currencies`() {
    val received = Money(BigDecimal("20.00"), Currency.Bs)
    val total = Money(BigDecimal("10.00"), Currency.Dollar)
    assertFailsWith<IllegalArgumentException> { ChangeAmount(received, total) }
  }
}
