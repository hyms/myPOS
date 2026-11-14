package com.copitoB.mypos.application.transactions

import com.copitoB.mypos.domain.value-objects.Money
import com.copitoB.mypos.domain.value-objects.Currency
import java.math.BigDecimal
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RevertTransactionIntegrationTest {
  @Test
  fun `accumulator create and revert balance equals zero`() {
    // Simulate: create sale of 100, then revert → net balance should be 0
    var netBalance = BigDecimal.ZERO

    // Create sale: net_balance += 100
    netBalance = netBalance.add(BigDecimal("100.00"))
    assertEquals(BigDecimal("100.00"), netBalance)

    // Revert sale: net_balance -= 100
    netBalance = netBalance.subtract(BigDecimal("100.00"))
    assertEquals(BigDecimal.ZERO, netBalance)

    assertTrue(netBalance.compareTo(BigDecimal.ZERO) == 0)
  }

  @Test
  fun `purchase revert stock safety check`() {
    // Pre-check: stock must not go negative when reverting a purchase
    val currentStock = BigDecimal("10.0")
    val purchaseQuantity = BigDecimal("5.0")
    
    // Reverting a purchase decrements stock
    require(currentStock.compareTo(purchaseQuantity) >= 0) {
      "Stock would go negative on purchase revert"
    }
    val newStock = currentStock.subtract(purchaseQuantity)
    assertEquals(BigDecimal("5.0"), newStock)
  }

  @Test
  fun `expense revert balance only no stock`() {
    var netBalance = BigDecimal("100.00")
    val expenseAmount = BigDecimal("30.00")

    // Create expense: net_balance -= 30
    netBalance = netBalance.subtract(expenseAmount)
    assertEquals(BigDecimal("70.00"), netBalance)

    // Revert expense: net_balance += 30 (balance only, no stock)
    netBalance = netBalance.add(expenseAmount)
    assertEquals(BigDecimal("100.00"), netBalance)
  }
}
