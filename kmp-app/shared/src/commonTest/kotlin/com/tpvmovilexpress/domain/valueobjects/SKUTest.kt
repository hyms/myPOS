package com.tpvmovilexpress.domain.valueobjects

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertThrows
import kotlin.test.assertTrue

class SKUTest {

    @Test
    fun `SKU.create normalizes input to uppercase`() {
        val sku = SKU.create("abc-123")
        assertEquals("ABC-123", sku.value)
    }

    @Test
    fun `SKU.create trims whitespace`() {
        val sku = SKU.create("  ABC-123  ")
        assertEquals("ABC-123", sku.value)
    }

    @Test
    fun `SKU.create accepts alphanumeric with hyphen`() {
        val sku = SKU.create("PRODUCT-001")
        assertEquals("PRODUCT-001", sku.value)
    }

    @Test
    fun `SKU.create accepts alphanumeric with underscore`()() {
        val sku = SKU.create("PRODUCT_001")
        assertEquals("PRODUCT_001", sku.value)
    }

    @Test
    fun `SKU.create accepts mixed hyphen and underscore`() {
        val sku = SKU.create("PRODUCT-001_VARIANT")
        assertEquals("PRODUCT-001_VARIANT", sku.value)
    }

    @Test
    fun `SKU.create throws for empty string`() {
        assertThrows<IllegalArgumentException> { SKU.create("") }
        assertThrows<IllegalArgumentException> { SKU.create("   ") }
    }

    @Test
    fun `SKU.create throws for too long string`() {
        val long = "A".repeat(33)
        assertThrows<IllegalArgumentException> { SKU.create(long) }
    }

    @Test
    fun `SKU.create throws for invalid characters`() {
        assertThrows<IllegalArgumentException> { SKU.create("PRODUCT 001") } // space
        assertThrows<IllegalArgumentException> { SKU.create("PRODUCT@001") } // @
        assertThrows<IllegalArgumentException> { SKU.create("PRODUCT.001") } // .
        assertThrows<IllegalArgumentException> { SKU.create("PRODUCT/001") } // /
        assertThrows<IllegalArgumentException> { SKU.create("PRODUCT#001") } // #
    }

    @Test
    fun `SKU.create accepts max length`() {
        val max = "A".repeat(32)
        val sku = SKU.create(max)
        assertEquals(max, sku.value)
    }

    @Test
    fun `SKU.equals compares values`() {
        val sku1 = SKU.create("ABC-123")
        val sku2 = SKU.create("ABC-123")
        val sku3 = SKU.create("XYZ-789")

        assertTrue(sku1.equals(sku2))
        assertTrue(!sku1.equals(sku3))
    }

    @Test
    fun `SKU.toString returns value`() {
        val sku = SKU.create("ABC-123")
        assertEquals("ABC-123", sku.toString())
    }

    @Test
    fun `String.toSKU extension works`() {
        val sku = "ABC-123".toSKU()
        assertEquals("ABC-123", sku.value)
    }
}