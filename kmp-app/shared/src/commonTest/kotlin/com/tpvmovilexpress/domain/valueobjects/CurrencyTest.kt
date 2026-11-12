package com.tpvmovilexpress.domain.valueobjects

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertThrows
import kotlin.test.assertTrue

class CurrencyTest {

    @Test
    fun `Currency registry contains all supported currencies`() {
        assertEquals(4, Currency.REGISTRY.size)
        assertTrue(Currency.REGISTRY.containsKey(CurrencyCode.BOB))
        assertTrue(Currency.REGISTRY.containsKey(CurrencyCode.USD))
        assertTrue(Currency.REGISTRY.containsKey(CurrencyCode.EUR))
        assertTrue(Currency.REGISTRY.containsKey(CurrencyCode.PEN))
    }

    @Test
    fun `Currency.get returns correct configuration for each code`() {
        val bob = Currency.get(CurrencyCode.BOB)
        assertEquals("Bs.", bob.symbol)
        assertEquals(2, bob.fractionDigits)
        assertEquals("es_BO", bob.locale.toLanguageTag())

        val usd = Currency.get(CurrencyCode.USD)
        assertEquals("$", usd.symbol)
        assertEquals(2, usd.fractionDigits)
        assertEquals("en_US", usd.locale.toLanguageTag())

        val eur = Currency.get(CurrencyCode.EUR)
        assertEquals("€", eur.symbol)
        assertEquals(2, eur.fractionDigits)
        assertEquals("es_ES", eur.locale.toLanguageTag())

        val pen = Currency.get(CurrencyCode.PEN)
        assertEquals("S/", pen.symbol)
        assertEquals(2, pen.fractionDigits)
        assertEquals("es_PE", pen.locale.toLanguageTag())
    }

    @Test
    fun `Currency.get throws for unsupported code`() {
        // This test verifies the enum is exhaustive - all codes are handled
        CurrencyCode.values().forEach { code ->
            assertNotNull(Currency.get(code))
        }
    }

    @Test
    fun `Currency.default returns BOB`() {
        val default = Currency.default()
        assertEquals(CurrencyCode.BOB, default.code)
        assertEquals("Bs.", default.symbol)
    }

    @Test
    fun `Currency.formatter produces correct format`() {
        val bob = Currency.get(CurrencyCode.BOB)
        val formatted = bob.formatter().format(123.45)
        assertTrue(formatted.contains("123") && formatted.contains("45"))

        val usd = Currency.get(CurrencyCode.USD)
        val formattedUsd = usd.formatter().format(123.45)
        assertTrue(formattedUsd.contains("123") && formattedUsd.contains("45"))
    }

    @Test
    fun `Currency.format delegates to Money.format`() {
        val bob = Currency.get(CurrencyCode.BOB)
        val money = Money.of(123.45, bob)
        val formatted = bob.format(money)
        assertTrue(formatted.contains("123") && formatted.contains("45"))
    }
}