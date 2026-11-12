package com.tpvmovilexpress.domain.rules

import com.tpvmovilexpress.domain.valueobjects.Currency
import com.tpvmovilexpress.domain.valueobjects.Money
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertThrows
import kotlin.test.assertTrue

class CartRulesTest {

    private val bob = Currency.get(CurrencyCode.BOB)
    private val usd = Currency.get(CurrencyCode.USD)

    @Test
    fun `selectUnitPrice returns sale price for SALE cart`() {
        val salePrice = Money.of(100.0, bob)
        val purchasePrice = Money.of(80.0, bob)

        val result = CartRules.selectUnitPrice(salePrice, purchasePrice, CartType.SALE)

        assertEquals(salePrice, result)
    }

    @Test
    fun `selectUnitPrice returns purchase price for PURCHASE cart`() {
        val salePrice = Money.of(100.0, bob)
        val purchasePrice = Money.of(80.0, bob)

        val result = CartRules.selectUnitPrice(salePrice, purchasePrice, CartType.PURCHASE)

        assertEquals(purchasePrice, result)
    }

    @Test
    fun `lineSubtotal calculates unitPrice * quantity`() {
        val line = CartRules.CartLine(
            unitPrice = Money.of(10.0, bob),
            quantity = 3.0,
            discount = Money.zero(bob)
        )

        val result = CartRules.lineSubtotal(line)

        assertEquals(30.0, result.amount)
    }

    @Test
    fun `lineSubtotal handles fractional quantity`() {
        val line = CartRules.CartLine(
            unitPrice = Money.of(10.0, bob),
            quantity = 2.5,
            discount = Money.zero(bob)
        )

        val result = CartRules.lineSubtotal(line)

        assertEquals(25.0, result.amount)
    }

    @Test
    fun `lineSubtotal throws for negative quantity`() {
        val line = CartRules.CartLine(
            unitPrice = Money.of(10.0, bob),
            quantity = -1.0,
            discount = Money.zero(bob)
        )

        assertThrows<IllegalArgumentException> { CartRules.lineSubtotal(line) }
    }

    @Test
    fun `lineSubtotal throws for non-finite quantity`() {
        val line = CartRules.CartLine(
            unitPrice = Money.of(10.0, bob),
            quantity = Double.NaN,
            discount = Money.zero(bob)
        )

        assertThrows<IllegalArgumentException> { CartRules.lineSubtotal(line) }
    }

    @Test
    fun `calcTotals calculates subtotal, discount, total`() {
        val lines = listOf(
            CartRules.CartLine(Money.of(100.0, bob), 2.0, Money.zero(bob)),
            CartRules.CartLine(Money.of(50.0, bob), 1.0, Money.of(10.0, bob))
        )

        val result = CartRules.calcTotals(lines, bob)

        assertEquals(250.0, result.subtotal.amount)  // 200 + 50
        assertEquals(10.0, result.discount.amount)
        assertEquals(240.0, result.total.amount)     // 250 - 10
    }

    @Test
    fun `calcTotals clamps discount to subtotal`() {
        val lines = listOf(
            CartRules.CartLine(Money.of(100.0, bob), 1.0, Money.of(150.0, bob))
        )

        val result = CartRules.calcTotals(lines, bob)

        assertEquals(100.0, result.subtotal.amount)
        assertEquals(100.0, result.discount.amount)  // clamped to subtotal
        assertEquals(0.0, result.total.amount)       // 100 - 100
    }

    @Test
    fun `calcTotals throws for empty cart`() {
        assertThrows<IllegalArgumentException> { CartRules.calcTotals(emptyList(), bob) }
    }

    @Test
    fun `calcTotals throws for currency mismatch in lines`() {
        val lines = listOf(
            CartRules.CartLine(Money.of(100.0, bob), 1.0, Money.zero(bob)),
            CartRules.CartLine(Money.of(50.0, usd), 1.0, Money.zero(usd))
        )

        assertThrows<IllegalArgumentException> { CartRules.calcTotals(lines, bob) }
    }

    @Test
    fun `calcTotals throws for currency mismatch with parameter`() {
        val lines = listOf(
            CartRules.CartLine(Money.of(100.0, usd), 1.0, Money.zero(usd))
        )

        assertThrows<IllegalArgumentException> { CartRules.calcTotals(lines, bob) }
    }

    @Test
    fun `applyDiscount clamps to [0, subtotal]`() {
        val subtotal = Money.of(100.0, bob)

        assertEquals(0.0, CartRules.applyDiscount(subtotal, Money.of(-10.0, bob)).amount)
        assertEquals(50.0, CartRules.applyDiscount(subtotal, Money.of(50.0, bob)).amount)
        assertEquals(100.0, CartRules.applyDiscount(subtotal, Money.of(150.0, bob)).amount)
    }

    @Test
    fun `calcChange calculates received - total`() {
        val total = Money.of(100.0, bob)
        val received = Money.of(150.0, bob)

        val change = CartRules.calcChange(total, received)

        assertEquals(50.0, change.amount)
    }

    @Test
    fun `calcChange handles exact payment`() {
        val total = Money.of(100.0, bob)
        val received = Money.of(100.0, bob)

        val change = CartRules.calcChange(total, received)

        assertEquals(0.0, change.amount)
    }

    @Test
    fun `calcChange handles insufficient payment`() {
        val total = Money.of(100.0, bob)
        val received = Money.of(50.0, bob)

        val change = CartRules.calcChange(total, received)

        assertEquals(-50.0, change.amount)
    }

    @Test
    fun `calcChange throws for currency mismatch`() {
        val total = Money.of(100.0, bob)
        val received = Money.of(150.0, usd)

        assertThrows<IllegalArgumentException> { CartRules.calcChange(total, received) }
    }
}