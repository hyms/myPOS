package com.tpvmovilexpress.domain.valueobjects

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertThrows
import kotlin.test.assertTrue

class PaymentMethodTest {

    @Test
    fun `PaymentMethod.fromCode creates correct instances`() {
        assertEquals(PaymentMethod.CASH, PaymentMethod.fromCode("CASH"))
        assertEquals(PaymentMethod.CASH, PaymentMethod.fromCode("cash"))
        assertEquals(PaymentMethod.CARD, PaymentMethod.fromCode("CARD"))
        assertEquals(PaymentMethod.TRANSFER, PaymentMethod.fromCode("TRANSFER"))
        assertEquals(PaymentMethod.QR, PaymentMethod.fromCode("QR"))
        assertEquals(PaymentMethod.QR, PaymentMethod.fromCode("qr"))
    }

    @Test
    fun `PaymentMethod.fromCode throws for invalid code`() {
        assertThrows<IllegalArgumentException> { PaymentMethod.fromCode("INVALID") }
        assertThrows<IllegalArgumentException> { PaymentMethod.fromCode("") }
    }

    @Test
    fun `PaymentMethod.all contains all methods`() {
        val all = PaymentMethod.all
        assertEquals(4, all.size)
        assertTrue(PaymentMethod.CASH in all)
        assertTrue(PaymentMethod.CARD in all)
        assertTrue(PaymentMethod.TRANSFER in all)
        assertTrue(PaymentMethod.QR in all)
    }

    @Test
    fun `PaymentMethod.isCash identifies cash`() {
        assertTrue(PaymentMethod.CASH.isCash)
        assertTrue(!PaymentMethod.CARD.isCash)
        assertTrue(!PaymentMethod.TRANSFER.isCash)
        assertTrue(!PaymentMethod.QR.isCash)
    }

    @Test
    fun `PaymentMethod.displayName returns Spanish names`() {
        assertEquals("Efectivo", PaymentMethod.CASH.displayName)
        assertEquals("Tarjeta", PaymentMethod.CARD.displayName)
        assertEquals("Transferencia", PaymentMethod.TRANSFER.displayName)
        assertEquals("QR", PaymentMethod.QR.displayName)
    }

    @Test
    fun `PaymentMethod.toString returns display name`() {
        assertEquals("Efectivo", PaymentMethod.CASH.toString())
        assertEquals("Tarjeta", PaymentMethod.CARD.toString())
    }

    @Test
    fun `PaymentMethod.code returns correct code`() {
        assertEquals("CASH", PaymentMethod.CASH.code)
        assertEquals("CARD", PaymentMethod.CARD.code)
        assertEquals("TRANSFER", PaymentMethod.TRANSFER.code)
        assertEquals("QR", PaymentMethod.QR.code)
    }
}