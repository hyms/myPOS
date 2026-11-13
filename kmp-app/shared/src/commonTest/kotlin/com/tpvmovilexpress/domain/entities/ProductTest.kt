package com.tpvmovilexpress.domain.entities

import com.tpvmovilexpress.domain.valueobjects.Currency
import com.tpvmovilexpress.domain.valueobjects.CurrencyCode
import com.tpvmovilexpress.domain.valueobjects.Money
import com.tpvmovilexpress.domain.valueobjects.SKU
import com.tpvmovilexpress.domain.valueobjects.Stock
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ProductTest {

    private val bob = Currency.get(CurrencyCode.BOB)

    private fun createTestProduct(
        id: Long = 1,
        categoryId: Long = 1,
        sku: SKU? = SKU.create("TEST-001"),
        name: String = "Test Product",
        sellingPrice: Double = 100.0,
        purchasePrice: Double = 50.0,
        currentStock: Double = 100.0,
        minStock: Double = 10.0,
        imageUri: String? = null,
        deletedAt: String? = null
    ) = Product(
        id = ProductId(id),
        categoryId = CategoryId(categoryId),
        sku = sku,
        name = name,
        sellingPrice = Money.of(sellingPrice, bob),
        purchasePrice = Money.of(purchasePrice, bob),
        currentStock = Stock.of(currentStock),
        minStock = Stock.of(minStock),
        imageUri = imageUri,
        registeredAt = "2024-01-01T00:00:00Z",
        updatedAt = "2024-01-01T00:00:00Z",
        deletedAt = deletedAt
    )

    // ─── Factory Tests ─────────────────────────

    @Test
    fun `create builds valid product from input`() {
        val input = CreateProductInput(
            categoryId = CategoryId(1),
            sku = "ABC-123",
            name = "Widget",
            sellingPrice = 25.50,
            purchasePrice = 12.00,
            currentStock = 100.0,
            minStock = 10.0,
            imageUri = null
        )

        val product = Product.create(input, bob, "2024-06-01T12:00:00Z")

        assertEquals("Widget", product.name)
        assertEquals(CategoryId(1), product.categoryId)
        assertEquals("ABC-123", product.sku?.value)
        assertEquals(25.50, product.sellingPrice.amount)
        assertEquals(12.00, product.purchasePrice.amount)
        assertEquals(100.0, product.currentStock.amount)
        assertEquals(10.0, product.minStock.amount)
        assertEquals("2024-06-01T12:00:00Z", product.registeredAt)
        assertNull(product.deletedAt)
    }

    @Test
    fun `create trims product name`() {
        val input = CreateProductInput(
            categoryId = CategoryId(1),
            sku = null,
            name = "  Trimmed Name  ",
            sellingPrice = 10.0,
            purchasePrice = 5.0,
            currentStock = 1.0,
            minStock = 0.0,
            imageUri = null
        )

        val product = Product.create(input, bob, "2024-01-01T00:00:00Z")

        assertEquals("Trimmed Name", product.name)
    }

    @Test
    fun `create throws for empty name`() {
        val input = CreateProductInput(
            categoryId = CategoryId(1),
            sku = null,
            name = "   ",
            sellingPrice = 10.0,
            purchasePrice = 5.0,
            currentStock = 1.0,
            minStock = 0.0,
            imageUri = null
        )

        assertFailsWith<IllegalArgumentException> {
            Product.create(input, bob, "2024-01-01T00:00:00Z")
        }
    }

    @Test
    fun `create throws for negative selling price`() {
        val input = CreateProductInput(
            categoryId = CategoryId(1),
            sku = null,
            name = "Test",
            sellingPrice = -1.0,
            purchasePrice = 5.0,
            currentStock = 1.0,
            minStock = 0.0,
            imageUri = null
        )

        assertFailsWith<IllegalArgumentException> {
            Product.create(input, bob, "2024-01-01T00:00:00Z")
        }
    }

    @Test
    fun `create throws for negative purchase price`() {
        val input = CreateProductInput(
            categoryId = CategoryId(1),
            sku = null,
            name = "Test",
            sellingPrice = 10.0,
            purchasePrice = -5.0,
            currentStock = 1.0,
            minStock = 0.0,
            imageUri = null
        )

        assertFailsWith<IllegalArgumentException> {
            Product.create(input, bob, "2024-01-01T00:00:00Z")
        }
    }

    @Test
    fun `create throws for negative current stock`() {
        val input = CreateProductInput(
            categoryId = CategoryId(1),
            sku = null,
            name = "Test",
            sellingPrice = 10.0,
            purchasePrice = 5.0,
            currentStock = -1.0,
            minStock = 0.0,
            imageUri = null
        )

        assertFailsWith<IllegalArgumentException> {
            Product.create(input, bob, "2024-01-01T00:00:00Z")
        }
    }

    @Test
    fun `create throws for negative min stock`() {
        val input = CreateProductInput(
            categoryId = CategoryId(1),
            sku = null,
            name = "Test",
            sellingPrice = 10.0,
            purchasePrice = 5.0,
            currentStock = 1.0,
            minStock = -1.0,
            imageUri = null
        )

        assertFailsWith<IllegalArgumentException> {
            Product.create(input, bob, "2024-01-01T00:00:00Z")
        }
    }

    @Test
    fun `create handles null SKU`() {
        val input = CreateProductInput(
            categoryId = CategoryId(1),
            sku = null,
            name = "No SKU Product",
            sellingPrice = 10.0,
            purchasePrice = 5.0,
            currentStock = 1.0,
            minStock = 0.0,
            imageUri = null
        )

        val product = Product.create(input, bob, "2024-01-01T00:00:00Z")

        assertNull(product.sku)
    }

    @Test
    fun `create handles empty SKU as null`() {
        val input = CreateProductInput(
            categoryId = CategoryId(1),
            sku = "   ",
            name = "Empty SKU Product",
            sellingPrice = 10.0,
            purchasePrice = 5.0,
            currentStock = 1.0,
            minStock = 0.0,
            imageUri = null
        )

        val product = Product.create(input, bob, "2024-01-01T00:00:00Z")

        assertNull(product.sku)
    }

    @Test
    fun `create normalizes SKU to uppercase`() {
        val input = CreateProductInput(
            categoryId = CategoryId(1),
            sku = "abc-123",
            name = "SKU Test",
            sellingPrice = 10.0,
            purchasePrice = 5.0,
            currentStock = 1.0,
            minStock = 0.0,
            imageUri = null
        )

        val product = Product.create(input, bob, "2024-01-01T00:00:00Z")

        assertEquals("ABC-123", product.sku?.value)
    }

    @Test
    fun `create accepts zero prices`() {
        val input = CreateProductInput(
            categoryId = CategoryId(1),
            sku = null,
            name = "Free Product",
            sellingPrice = 0.0,
            purchasePrice = 0.0,
            currentStock = 0.0,
            minStock = 0.0,
            imageUri = null
        )

        val product = Product.create(input, bob, "2024-01-01T00:00:00Z")

        assertEquals(0.0, product.sellingPrice.amount)
        assertEquals(0.0, product.purchasePrice.amount)
        assertEquals(0.0, product.currentStock.amount)
        assertEquals(0.0, product.minStock.amount)
    }

    @Test
    fun `create handles fractional stock`() {
        val input = CreateProductInput(
            categoryId = CategoryId(1),
            sku = null,
            name = "Fractional Stock",
            sellingPrice = 10.0,
            purchasePrice = 5.0,
            currentStock = 123.456,
            minStock = 0.001,
            imageUri = null
        )

        val product = Product.create(input, bob, "2024-01-01T00:00:00Z")

        assertEquals(123.456, product.currentStock.amount)
        assertEquals(0.001, product.minStock.amount)
    }

    // ─── Property Tests ────────────────────────

    @Test
    fun `isLowStock returns true when current below min`() {
        val product = createTestProduct(currentStock = 5.0, minStock = 10.0)
        assertTrue(product.isLowStock)
    }

    @Test
    fun `isLowStock returns false when current equals min`() {
        val product = createTestProduct(currentStock = 10.0, minStock = 10.0)
        assertFalse(product.isLowStock)
    }

    @Test
    fun `isLowStock returns false when current above min`() {
        val product = createTestProduct(currentStock = 15.0, minStock = 10.0)
        assertFalse(product.isLowStock)
    }

    @Test
    fun `isDeleted returns true when deletedAt is set`() {
        val product = createTestProduct(deletedAt = "2024-06-01T12:00:00Z")
        assertTrue(product.isDeleted)
    }

    @Test
    fun `isDeleted returns false when deletedAt is null`() {
        val product = createTestProduct(deletedAt = null)
        assertFalse(product.isDeleted)
    }

    // ─── Immutability Tests ────────────────────

    @Test
    fun `Product is immutable via copy`() {
        val original = createTestProduct(name = "Original")
        val modified = original.copy(name = "Modified")

        assertEquals("Original", original.name)
        assertEquals("Modified", modified.name)
    }

    @Test
    fun `Product equality is by value`() {
        val a = createTestProduct()
        val b = createTestProduct()

        assertEquals(a, b)
    }
}
