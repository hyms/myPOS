package com.tpvmovilexpress.domain.rules

import com.tpvmovilexpress.domain.valueobjects.CurrencyCode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertThrows

class SettingsRulesTest {

    @Test
    fun `validatePageSize accepts valid range`() {
        assertEquals(5, SettingsRules.validatePageSize(5))
        assertEquals(15, SettingsRules.validatePageSize(15))
        assertEquals(50, SettingsRules.validatePageSize(50))
        assertEquals(100, SettingsRules.validatePageSize(100))
    }

    @Test
    fun `validatePageSize throws for below minimum`() {
        assertThrows<IllegalArgumentException> { SettingsRules.validatePageSize(4) }
        assertThrows<IllegalArgumentException> { SettingsRules.validatePageSize(1) }
        assertThrows<IllegalArgumentException> { SettingsRules.validatePageSize(0) }
        assertThrows<IllegalArgumentException> { SettingsRules.validatePageSize(-1) }
    }

    @Test
    fun `validatePageSize throws for above maximum`() {
        assertThrows<IllegalArgumentException> { SettingsRules.validatePageSize(101) }
        assertThrows<IllegalArgumentException> { SettingsRules.validatePageSize(200) }
    }

    @Test
    fun `validateCurrencyCode accepts valid codes`() {
        assertEquals(CurrencyCode.BOB, SettingsRules.validateCurrencyCode("BOB"))
        assertEquals(CurrencyCode.BOB, SettingsRules.validateCurrencyCode("bob"))
        assertEquals(CurrencyCode.USD, SettingsRules.validateCurrencyCode("USD"))
        assertEquals(CurrencyCode.USD, SettingsRules.validateCurrencyCode("usd"))
        assertEquals(CurrencyCode.EUR, SettingsRules.validateCurrencyCode("EUR"))
        assertEquals(CurrencyCode.PEN, SettingsRules.validateCurrencyCode("PEN"))
    }

    @Test
    fun `validateCurrencyCode throws for invalid code`() {
        assertThrows<IllegalArgumentException> { SettingsRules.validateCurrencyCode("INVALID") }
        assertThrows<IllegalArgumentException> { SettingsRules.validateCurrencyCode("") }
        assertThrows<IllegalArgumentException> { SettingsRules.validateCurrencyCode("XYZ") }
    }
}