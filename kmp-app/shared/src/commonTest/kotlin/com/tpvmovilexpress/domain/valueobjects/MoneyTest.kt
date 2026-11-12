package com.tpvmovilexpress.domain.valueobjects

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertThrows
import kotlin.test.assertTrue

class MoneyTest {

    private val bob = Currency.get(CurrencyCode.BOB)
    private val usd = Currency.get(CurrencyCode.USD)

    @Test
    fun `Money.of creates from major units`() {
        val money = Money.of(123.45, bob)
        assertEquals(12345L, money.amountCents)
        assertEquals(123.45, money.amount)
    }

    @Test
    fun `Money.fromCents creates from minor units`() {
        val money = Money.fromCents(12345L, bob)
        assertEquals(12345L, money.amountCents)
        assertEquals(123.45, money.amount)
    }

    @Test
    fun `Money.zero creates zero amount`() {
        val money = Money.zero(bob)
        assertEquals(0L, money.amountCents)
        assertEquals(0.0, money.amount)
        assertTrue(money.isZero)
    }

    @Test
    fun `Money.parse creates from string`() {
        val money = Money.parse("123.45", bob)
        assertEquals(12345L, money.amountCents)
        assertEquals(123.45, money.amount)
    }

    @Test
    fun `Money.parse handles trailing zeros`() {
        val money = Money.parse("100.00", bob)
        assertEquals(10000L, money.amountCents)
    }

    @Test
    fun `Money.of throws for non-finite amount`() {
        assertThrows<IllegalArgumentException> { Money.of(Double.NaN, bob) }
        assertThrows<IllegalArgumentException> { Money.of(Double.POSITIVE_INFINITY, bob) }
        assertThrows<IllegalArgumentException> { Money.of(Double.NEGATIVE_INFINITY, bob) }
    }

    @Test
    fun `Money.plus adds amounts with same currency`() {
        val a = Money.of(100.00, bob)
        val b = Money.of(50.50, bob)
        val result = a + b
        assertEquals(15050L, result.amountCents)
        assertEquals(150.50, result.amount)
    }

    @Test
    fun `Money.plus throws for currency mismatch`() {
        val a = Money.of(100.00, bob)
        val b = Money.of(50.00, usd)
        assertThrows<IllegalArgumentException> { a + b }
    }

    @Test
    fun `Money.minus subtracts amounts with same currency`() {
        val a = Money.of(100.00, bob)
        val b = Money.of(30.25, bob)
        val result = a - b
        assertEquals(6975L, result.amountCents)
        assertEquals(69.75, result.amount)
    }

    @Test
    fun `Money.minus allows negative result`() {
        val a = Money.of(50.00, bob)
        val b = Money.of(100.00, bob)
        val result = a - b
        assertEquals(-5000L, result.amountCents)
        assertEquals(-50.00, result.amount)
        assertTrue(result.isNegative)
    }

    @Test
    fun `Money.times multiplies by factor`() {
        val money = Money.of(100.00, bob)
        val result = money.times(1.5)
        assertEquals(15000L, result.amountCents)
        assertEquals(150.00, result.amount)
    }

    @Test
    fun `Money.times rounds using banker rounding`() {
        // 100.00 * 1.333 = 133.3, rounds to 133.30 (HALF_EVEN)
        val money = Money.of(100.00, bob)
        val result = money.times(1.333)
        assertEquals(13330L, result.amountCents)
    }

    @Test
    fun `Money.times throws for non-finite factor`() {
        val money = Money.of(100.00, bob)
        assertThrows<IllegalArgumentException> { money.times(Double.NaN) }
        assertThrows<IllegalArgumentException> { money.times(Double.POSITIVE_INFINITY) }
    }

    @Test
    fun `Money.div divides by divisor`() {
        val money = Money.of(100.00, bob)
        val result = money.div(4.0)
        assertEquals(2500L, result.amountCents)
        assertEquals(25.00, result.amount)
    }

    @Test
    fun `Money.div throws for zero divisor`() {
        val money = Money.of(100.00, bob)
        assertThrows<ArithmeticException> { money.div(0.0) }
    }

    @Test
    fun `Money.abs returns absolute value`() {
        val positive = Money.of(100.00, bob)
        val negative = Money.of(-100.00, bob)

        assertEquals(positive.amountCents, positive.abs().amountCents)
        assertEquals(positive.amountCents, negative.abs().amountCents)
    }

    @Test
    fun `Money.unaryMinus negates amount`() {
        val money = Money.of(100.00, bob)
        val negated = -money
        assertEquals(-10000L, negated.amountCents)
        assertTrue(negated.isNegative)
    }

    @Test
    fun `Money.isZero checks zero amount`() {
        assertTrue(Money.zero(bob).isZero)
        assertTrue((-Money.zero(bob)).isZero)
    }

    @Test
    fun `Money.isPositive checks positive amount`() {
        assertTrue(Money.of(100.00, bob).isPositive)
        assertTrue(!Money.zero(bob).isPositive)
        assertTrue(!Money.of(-100.00, bob).isPositive)
    }

    @Test
    fun `Money.isNegative checks negative amount`() {
        assertTrue(Money.of(-100.00, bob).isNegative)
        assertTrue(!Money.zero(bob).isNegative)
        assertTrue(!Money.of(100.00, bob).isNegative)
    }

    @Test
    fun `Money.compareTo orders by amount`() {
        val a = Money.of(50.00, bob)
        val b = Money.of(100.00, bob)
        val c = Money.of(100.00, bob)

        assertTrue(a.compareTo(b) < 0)
        assertTrue(b.compareTo(a) > 0)
        assertEquals(0, b.compareTo(c))
    }

    @Test
    fun `Money.compareTo throws for currency mismatch`() {
        val a = Money.of(100.00, bob)
        val b = Money.of(100.00, usd)
        assertThrows<IllegalArgumentException> { a.compareTo(b) }
    }

    @Test
    fun `Money.format produces formatted string`() {
        val money = Money.of(1234.56, bob)
        val formatted = money.format()
        assertTrue(formatted.contains("1234") && formatted.contains("56"))
    }

    @Test
    fun `Money.toString includes symbol and formatted amount`() {
        val money = Money.of(100.00, bob)
        val str = money.toString()
        assertTrue(str.contains("Bs."))
        assertTrue(str.contains("100"))
    }

    @Test
    fun `Money round2 is no-op`() {
        val money = Money.of(100.00, bob)
        assertEquals(money, money.round2())
    }
}