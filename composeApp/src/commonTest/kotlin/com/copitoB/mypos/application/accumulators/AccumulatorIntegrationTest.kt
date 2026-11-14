package com.copitoB.mypos.application.accumulators

import com.copitoB.mypos.domain.value-objects.Money
import com.copitoB.mypos.domain.value-objects.Currency
import java.math.BigDecimal
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AccumulatorIntegrationTest {
  @Test
  fun `sales increase accumulator balance`() {
    var netBalance = Money(BigDecimal("0.00"), Currency.Bs)
    val sale = Money(BigDecimal("100.00"), Currency.Bs)
    netBalance = netBalance.plus(sale)
    assertEquals(BigDecimal("100.00"), netBalance.amount)
  }

  @Test
  fun `purchases decrease accumulator balance`() {
    var netBalance = Money(BigDecimal("100.00"), Currency.Bs)
    val purchase = Money(BigDecimal("30.00"), Currency.Bs)
    netBalance = netBalance.minus(purchase)
    assertEquals(BigDecimal("70.00"), netBalance.amount)
  }

  @Test
  fun `expenses decrease accumulator balance`() {
    var netBalance = Money(BigDecimal("100.00"), Currency.Bs)
    val expense = Money(BigDecimal("20.00"), Currency.Bs)
    netBalance = netBalance.minus(expense)
    assertEquals(BigDecimal("80.00"), netBalance.amount)
  }

  @Test
  fun `create and revert returns balance to zero`() {
    var netBalance = Money(BigDecimal("0.00"), Currency.Bs)
    
    // Create sale
    netBalance = netBalance.plus(Money(BigDecimal("100.00"), Currency.Bs))
    assertEquals(BigDecimal("100.00"), netBalance.amount)
    
    // Revert sale
    netBalance = netBalance.minus(Money(BigDecimal("100.00"), Currency.Bs))
    assertTrue(netBalance.isZero())
  }
}
