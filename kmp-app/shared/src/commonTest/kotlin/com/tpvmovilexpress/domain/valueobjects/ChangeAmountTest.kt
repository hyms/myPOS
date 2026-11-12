package com.tpvmovilexpress.domain.valueobjects

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertThrows
import kotlin.test.assertTrue

class ChangeAmountTest {

    private val bob = Currency.get(CurrencyCode.BOB)

    @Test
    fun `ChangeAmount.zero creates zero change`() {
        val change = ChangeAmount.zero(bob)
        assertEquals(0L, change.amount.amountCents)
        assertTrue(change.isSufficient)
        assertEquals("success", change.colorTheme)
    }

    @Test
    fun `ChangeAmount.calculate computes received - total`() {
        val total = Money.of(100.00, bob)
        val received = Money.of(150.00, bob)
        val change = ChangeAmount.calculate(received, total)

        assertEquals(5000L, change.amount.amountCents)
        assertEquals(50.00, change.amount.amount)
        assertTrue(change.isSufficient)
        assertEquals("success", change.colorTheme)
    }

    @Test
    fun `ChangeAmount.calculate handles exact change`() {
        val total = Money.of(100.00, bob)
        val received = Money.of(100.00, bob)
        val change = ChangeAmount.calculate(received, total)

        assertEquals(0L, change.amount.amountCents)
        assertTrue(change.isSufficient)
    }

    @Test
    fun `ChangeAmount.calculate handles insufficient change`() {
        val total = Money.of(100.00, bob)
        val received = Money.of(50.00, bob)
        val change = ChangeAmount.calculate(received, total)

        assertEquals(-5000L, change.amount.amountCents)
        assertEquals(-50.00, change.amount.amount)
        assertTrue(!change.isSufficient)
        assertEquals("danger", change.colorTheme)
    }

    @Test
    fun `ChangeAmount.calculate throws for currency mismatch`() {
        val usd = Currency.get(CurrencyCode.USD)
        val total = Money.of(100.00, bob)
        val received = Money.of(150.00, usd)
        assertThrows<IllegalArgumentException> { ChangeAmount.calculate(received, total) }
    }

    @Test
    fun `ChangeAmount.displayAmount returns absolute for insufficient`() {
        val total = Money.of(100.00, bob)
        val received = Money.of(50.00, bob)
        val change = ChangeAmount.calculate(received, total)

        assertEquals(5000L, change.displayAmount.amountCents)
        assertEquals(50.00, change.displayAmount.amount)
    }

    @Test
    fun `ChangeAmount.displayAmount returns actual for sufficient`() {
        val total = Money.of(100.00, bob)
        val received = Money.of(150.00, bob)
        val change = ChangeAmount.calculate(received, total)

        assertEquals(5000L, change.displayAmount.amountCents)
    }

    @Test
    fun `ChangeAmount.format includes sign`() {
        val sufficient = ChangeAmount.calculate(Money.of(150.00, bob), Money.of(100.00, bob))
        val insufficient = ChangeAmount.calculate(Money.of(50.00, bob), Money.of(100.00, bob))

        assertTrue(sufficient.format().startsWith("+"))
        assertTrue(insufficient.format().startsWith("-"))
    }
}