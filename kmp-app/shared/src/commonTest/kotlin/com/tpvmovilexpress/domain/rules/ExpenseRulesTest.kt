package com.tpvmovilexpress.domain.rules

import com.tpvmovilexpress.domain.valueobjects.Currency
import com.tpvmovilexpress.domain.valueobjects.Money
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertThrows

class ExpenseRulesTest {

    private val bob = Currency.get(CurrencyCode.BOB)

    @Test
    fun `validateAmount accepts positive amount`() {
        val amount = Money.of(100.0, bob)
        val result = ExpenseRules.validateAmount(amount)
        assertEquals(amount, result)
    }

    @Test
    fun `validateAmount throws for zero`() {
        val amount = Money.zero(bob)
        assertThrows<IllegalArgumentException> { ExpenseRules.validateAmount(amount) }
    }

    @Test
    fun `validateAmount throws for negative`() {
        val amount = Money.of(-100.0, bob)
        assertThrows<IllegalArgumentException> { ExpenseRules.validateAmount(amount) }
    }

    @Test
    fun `validateDescription accepts non-empty string`() {
        val desc = "  Office supplies  "
        val result = ExpenseRules.validateDescription(desc)
        assertEquals("Office supplies", result)
    }

    @Test
    fun `validateDescription throws for empty string`() {
        assertThrows<IllegalArgumentException> { ExpenseRules.validateDescription("") }
        assertThrows<IllegalArgumentException> { ExpenseRules.validateDescription("   ") }
    }

    @Test
    fun `roundAmount rounds to 2 decimal places`() {
        assertEquals(123.45, ExpenseRules.roundAmount(123.456))
        assertEquals(123.46, ExpenseRules.roundAmount(123.4567))
        assertEquals(100.0, ExpenseRules.roundAmount(100.0))
        assertEquals(0.01, ExpenseRules.roundAmount(0.005))
        assertEquals(0.0, ExpenseRules.roundAmount(0.004))
    }

    @Test
    fun `roundAmount uses banker rounding for exact halves`() {
        // Banker's rounding: 0.005 -> 0.00 (even), 0.015 -> 0.02 (even)
        assertEquals(0.0, ExpenseRules.roundAmount(0.005))
        assertEquals(0.02, ExpenseRules.roundAmount(0.015))
        assertEquals(1.0, ExpenseRules.roundAmount(0.995))
        assertEquals(1.02, ExpenseRules.roundAmount(1.015))
    }

    @Test
    fun `roundAmount throws for non-finite`() {
        assertThrows<IllegalArgumentException> { ExpenseRules.roundAmount(Double.NaN) }
        assertThrows<IllegalArgumentException> { ExpenseRules.roundAmount(Double.POSITIVE_INFINITY) }
    }
}