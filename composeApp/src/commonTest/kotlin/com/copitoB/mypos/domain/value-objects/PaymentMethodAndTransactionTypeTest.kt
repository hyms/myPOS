package com.copitoB.mypos.domain.value-objects

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PaymentMethodTest {
  @Test
  fun `defines all payment methods`() {
    assertEquals(4, PaymentMethod.values().size)
    assertEquals(PaymentMethod.CASH, PaymentMethod.valueOf("CASH"))
    assertEquals(PaymentMethod.CARD, PaymentMethod.valueOf("CARD"))
    assertEquals(PaymentMethod.TRANSFER, PaymentMethod.valueOf("TRANSFER"))
    assertEquals(PaymentMethod.QR, PaymentMethod.valueOf("QR"))
  }

  @Test
  fun `identifies cash payment method`() {
    assertTrue(PaymentMethod.CASH.isCash())
    assertTrue(!PaymentMethod.CARD.isCash())
    assertTrue(!PaymentMethod.TRANSFER.isCash())
    assertTrue(!PaymentMethod.QR.isCash())
  }
}

class TransactionTypeTest {
  @Test
  fun `defines all transaction types`() {
    assertEquals(3, TransactionType.values().size)
    assertEquals(TransactionType.SALE, TransactionType.valueOf("SALE"))
    assertEquals(TransactionType.PURCHASE, TransactionType.valueOf("PURCHASE"))
    assertEquals(TransactionType.EXPENSE, TransactionType.valueOf("EXPENSE"))
  }
}
