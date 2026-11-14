package com.copitoB.mypos.domain.value-objects

import java.math.BigDecimal
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class CurrencyTest {
  @Test
  fun `parses valid currency codes`() {
    assertEquals(Currency.Bs, Currency.fromCode("Bs"))
    assertEquals(Currency.Dollar, Currency.fromCode("$"))
    assertEquals(Currency.Euro, Currency.fromCode("€"))
  }

  @Test
  fun `rejects invalid currency code`() {
    assertFailsWith<IllegalArgumentException> { Currency.fromCode("XYZ") }
  }

  @Test
  fun `formats currency`() {
    val money = Money(BigDecimal("10.50"), Currency.Bs)
    val formatted = Currency.Bs.format(money.amount)
    assertTrue(formatted.contains("10.50"))
  }
}
