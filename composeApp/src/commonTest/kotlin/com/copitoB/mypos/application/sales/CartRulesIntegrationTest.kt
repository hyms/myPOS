package com.copitoB.mypos.application.sales

import com.copitoB.mypos.domain.entities.ICartItem
import com.copitoB.mypos.domain.entities.SimpleCartItem
import com.copitoB.mypos.domain.value-objects.Currency
import com.copitoB.mypos.domain.value-objects.Money
import com.copitoB.mypos.domain.rules.CartRules
import java.math.BigDecimal
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CartRulesIntegrationTest {
  @Test
  fun `calculates cart totals correctly`() {
    val items = listOf(
      SimpleCartItem(productId = 1, productName = "Product A", quantity = 2.0, unitPrice = 10.0),
      SimpleCartItem(productId = 2, productName = "Product B", quantity = 1.0, unitPrice = 5.0)
    )
    val totals = CartRules.calcTotals(items)
    assertEquals(25.0, totals.subtotal)
    assertEquals(0.0, totals.discount)
    assertEquals(25.0, totals.total)
  }

  @Test
  fun `applies discount correctly`() {
    val items = listOf(
      SimpleCartItem(productId = 1, productName = "Product A", quantity = 1.0, unitPrice = 100.0)
    )
    val totals = CartRules.calcTotals(items, discount = 20.0)
    assertEquals(100.0, totals.subtotal)
    assertEquals(20.0, totals.discount)
    assertEquals(80.0, totals.total)
  }

  @Test
  fun `clamps discount to subtotal`() {
    val items = listOf(
      SimpleCartItem(productId = 1, productName = "Product A", quantity = 1.0, unitPrice = 50.0)
    )
    val totals = CartRules.calcTotals(items, discount = 100.0)
    assertEquals(50.0, totals.discount)
    assertEquals(0.0, totals.total)
  }

  @Test
  fun `calculates change correctly`() {
    val total = Money(BigDecimal("25.00"), Currency.Bs)
    val received = Money(BigDecimal("50.00"), Currency.Bs)
    val change = CartRules.calcChange(total = total, received = received)
    assertTrue(change.isSufficient())
    assertEquals(BigDecimal("25.00"), change.change.amount)
  }

  @Test
  fun `detects insufficient funds`() {
    val total = Money(BigDecimal("50.00"), Currency.Bs)
    val received = Money(BigDecimal("25.00"), Currency.Bs)
    val change = CartRules.calcChange(total = total, received = received)
    assertTrue(!change.isSufficient())
  }
}
