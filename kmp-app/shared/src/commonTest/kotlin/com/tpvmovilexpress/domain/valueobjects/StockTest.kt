package com.tpvmovilexpress.domain.valueobjects

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertThrows
import kotlin.test.assertTrue

class StockTest {

    @Test
    fun `Stock.of creates from decimal amount`() {
        val stock = Stock.of(123.456)
        assertEquals(123456L, stock.amountMilliunits)
        assertEquals(123.456, stock.amount)
    }

    @Test
    fun `Stock.fromMilliunits creates from thousandths`() {
        val stock = Stock.fromMilliunits(123456L)
        assertEquals(123456L, stock.amountMilliunits)
        assertEquals(123.456, stock.amount)
    }

    @Test
    fun `Stock.zero creates zero stock`() {
        val stock = Stock.zero()
        assertEquals(0L, stock.amountMilliunits)
        assertEquals(0.0, stock.amount)
        assertTrue(stock.isZero)
    }

    @Test
    fun `Stock.parse creates from string`() {
        val stock = Stock.parse("123.456")
        assertEquals(123456L, stock.amountMilliunits)
        assertEquals(123.456, stock.amount)
    }

    @Test
    fun `Stock.parse handles trailing zeros`() {
        val stock = Stock.parse("100.000")
        assertEquals(100000L, stock.amountMilliunits)
    }

    @Test
    fun `Stock.of throws for negative amount`() {
        assertThrows<IllegalArgumentException> { Stock.of(-1.0) }
        assertThrows<IllegalArgumentException> { Stock.of(-0.001) }
    }

    @Test
    fun `Stock.of throws for non-finite amount`() {
        assertThrows<IllegalArgumentException> { Stock.of(Double.NaN) }
        assertThrows<IllegalArgumentException> { Stock.of(Double.POSITIVE_INFINITY) }
    }

    @Test
    fun `Stock.fromMilliunits throws for negative`() {
        assertThrows<IllegalArgumentException> { Stock.fromMilliunits(-1) }
    }

    @Test
    fun `Stock.plus adds quantities`() {
        val a = Stock.of(100.500)
        val b = Stock.of(50.250)
        val result = a + b
        assertEquals(150750L, result.amountMilliunits)
        assertEquals(150.750, result.amount)
    }

    @Test
    fun `Stock.minus subtracts quantities`() {
        val a = Stock.of(100.500)
        val b = Stock.of(30.250)
        val result = a - b
        assertEquals(70250L, result.amountMilliunits)
        assertEquals(70.250, result.amount)
    }

    @Test
    fun `Stock.minus throws when result would be negative`() {
        val a = Stock.of(50.000)
        val b = Stock.of(100.000)
        assertThrows<IllegalArgumentException> { a - b }
    }

    @Test
    fun `Stock.isBelow checks threshold`() {
        val current = Stock.of(50.000)
        val min = Stock.of(100.000)
        assertTrue(current.isBelow(min))
        assertTrue(!min.isBelow(current))
        assertTrue(!current.isBelow(current))
    }

    @Test
    fun `Stock.max returns maximum`() {
        val a = Stock.of(100.000)
        val b = Stock.of(50.000)
        assertEquals(a, a.max(b))
        assertEquals(a, b.max(a))
    }

    @Test
    fun `Stock.min returns minimum`() {
        val a = Stock.of(100.000)
        val b = Stock.of(50.000)
        assertEquals(b, a.min(b))
        assertEquals(b, b.min(a))
    }

    @Test
    fun `Stock.compareTo orders by amount`() {
        val a = Stock.of(50.000)
        val b = Stock.of(100.000)
        val c = Stock.of(100.000)

        assertTrue(a.compareTo(b) < 0)
        assertTrue(b.compareTo(a) > 0)
        assertEquals(0, b.compareTo(c))
    }

    @Test
    fun `Stock.toString returns decimal representation`() {
        val stock = Stock.of(123.456)
        assertEquals("123.456", stock.toString())
    }
}