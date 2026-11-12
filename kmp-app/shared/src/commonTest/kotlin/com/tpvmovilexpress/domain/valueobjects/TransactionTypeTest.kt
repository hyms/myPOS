package com.tpvmovilexpress.domain.valueobjects

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertThrows
import kotlin.test.assertTrue

class TransactionTypeTest {

    @Test
    fun `TransactionType.fromCode creates correct instances`() {
        assertEquals(TransactionType.SALE, TransactionType.fromCode("SALE"))
        assertEquals(TransactionType.SALE, TransactionType.fromCode("sale"))
        assertEquals(TransactionType.PURCHASE, TransactionType.fromCode("PURCHASE"))
        assertEquals(TransactionType.EXPENSE, TransactionType.fromCode("EXPENSE"))
    }

    @Test
    fun `TransactionType.fromCode throws for invalid code`() {
        assertThrows<IllegalArgumentException> { TransactionType.fromCode("INVALID") }
        assertThrows<IllegalArgumentException> { TransactionType.fromCode("") }
    }

    @Test
    fun `TransactionType.all contains all types`() {
        val all = TransactionType.all
        assertEquals(3, all.size)
        assertTrue(TransactionType.SALE in all)
        assertTrue(TransactionType.PURCHASE in all)
        assertTrue(TransactionType.EXPENSE in all)
    }

    @Test
    fun `TransactionType.affectsStock identifies stock-affecting types`() {
        assertTrue(TransactionType.SALE.affectsStock)
        assertTrue(TransactionType.PURCHASE.affectsStock)
        assertTrue(!TransactionType.EXPENSE.affectsStock)
    }

    @Test
    fun `TransactionType.stockDirection returns correct direction`() {
        assertEquals(-1, TransactionType.SALE.stockDirection)
        assertEquals(1, TransactionType.PURCHASE.stockDirection)
        assertEquals(0, TransactionType.EXPENSE.stockDirection)
    }

    @Test
    fun `TransactionType.accumulatorField returns correct field`() {
        assertEquals("total_sales", TransactionType.SALE.accumulatorField)
        assertEquals("total_purchases", TransactionType.PURCHASE.accumulatorField)
        assertEquals("total_expenses", TransactionType.EXPENSE.accumulatorField)
    }

    @Test
    fun `TransactionType.balanceDirection returns correct direction`() {
        assertEquals(1, TransactionType.SALE.balanceDirection)
        assertEquals(-1, TransactionType.PURCHASE.balanceDirection)
        assertEquals(-1, TransactionType.EXPENSE.balanceDirection)
    }

    @Test
    fun `TransactionType.displayName returns Spanish names`() {
        assertEquals("Venta", TransactionType.SALE.displayName)
        assertEquals("Compra", TransactionType.PURCHASE.displayName)
        assertEquals("Gasto", TransactionType.EXPENSE.displayName)
    }

    @Test
    fun `TransactionType.toString returns display name`() {
        assertEquals("Venta", TransactionType.SALE.toString())
        assertEquals("Compra", TransactionType.PURCHASE.toString())
        assertEquals("Gasto", TransactionType.EXPENSE.toString())
    }

    @Test
    fun `TransactionType.code returns correct code`() {
        assertEquals("SALE", TransactionType.SALE.code)
        assertEquals("PURCHASE", TransactionType.PURCHASE.code)
        assertEquals("EXPENSE", TransactionType.EXPENSE.code)
    }
}