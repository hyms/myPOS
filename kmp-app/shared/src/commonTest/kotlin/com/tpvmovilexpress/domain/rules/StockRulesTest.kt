package com.tpvmovilexpress.domain.rules

import com.tpvmovilexpress.domain.entities.Product
import com.tpvmovilexpress.domain.entities.ProductId
import com.tpvmovilexpress.domain.entities.CategoryId
import com.tpvmovilexpress.domain.valueobjects.Currency
import com.tpvmovilexpress.domain.valueobjects.Money
import com.tpvmovilexpress.domain.valueobjects.SKU
import com.tpvmovilexpress.domain.valueobjects.Stock
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertThrows
import kotlin.test.assertTrue

class StockRulesTest {

    private val bob = Currency.get(CurrencyCode.BOB)
    private val sku = SKU.create("TEST-001")

    private fun createProduct(
        currentStock: Stock = Stock.of(100.0),
        minStock: Stock = Stock.of(10.0)
    ): Product {
        return Product(
            id = ProductId(1),
            categoryId = CategoryId(1),
            sku = sku,
            name = "Test Product",
            sellingPrice = Money.of(100.0, bob),
            purchasePrice = Money.of(50.0, bob),
            currentStock = currentStock,
            minStock = minStock,
            imageUri = null,
            registeredAt = "2024-01-01T00:00:00Z",
            updatedAt = "2024-01-01T00:00:00Z",
            deletedAt = null
        )
    }

    @Test
    fun `validateStockAvailable returns remaining stock when sufficient`() {
        val product = createProduct(currentStock = Stock.of(100.0))
        val requested = Stock.of(30.0)

        val remaining = StockRules.validateStockAvailable(product, requested)

        assertEquals(70.0, remaining.amount)
    }

    @Test
    fun `validateStockAvailable throws when insufficient`() {
        val product = createProduct(currentStock = Stock.of(50.0))
        val requested = Stock.of(100.0)

        val exception = assertThrows<StockRules.InsufficientStockException> {
            StockRules.validateStockAvailable(product, requested)
        }

        assertEquals(ProductId(1), exception.productId)
        assertEquals("Test Product", exception.productName)
        assertEquals(50.0, exception.available.amount)
        assertEquals(100.0, exception.requested.amount)
    }

    @Test
    fun `validateStockAvailable throws when exactly at limit`() {
        val product = createProduct(currentStock = Stock.of(50.0))
        val requested = Stock.of(50.0)

        val remaining = StockRules.validateStockAvailable(product, requested)

        assertEquals(0.0, remaining.amount)
    }

    @Test
    fun `isBelowMin returns true when current below min`() {
        val product = createProduct(currentStock = Stock.of(5.0), minStock = Stock.of(10.0))
        assertTrue(StockRules.isBelowMin(product))
    }

    @Test
    fun `isBelowMin returns false when current equals min`() {
        val product = createProduct(currentStock = Stock.of(10.0), minStock = Stock.of(10.0))
        assertTrue(!StockRules.isBelowMin(product))
    }

    @Test
    fun `isBelowMin returns false when current above min`() {
        val product = createProduct(currentStock = Stock.of(15.0), minStock = Stock.of(10.0))
        assertTrue(!StockRules.isBelowMin(product))
    }

    @Test
    fun `validateNonNegative accepts zero and positive`() {
        assertEquals(Stock.zero(), StockRules.validateNonNegative(Stock.zero()))
        assertEquals(Stock.of(10.0), StockRules.validateNonNegative(Stock.of(10.0)))
    }

    @Test
    fun `validateNonNegative throws for negative`() {
        // Can't create negative stock directly, but we can test the rule logic
        val exception = assertThrows<IllegalArgumentException> {
            StockRules.validateNonNegative(Stock.fromMilliunits(-1))
        }
        assertTrue(exception.message!!.contains("negative"))
    }

    @Test
    fun `validateMinStock accepts zero and positive`() {
        assertEquals(Stock.zero(), StockRules.validateMinStock(Stock.zero()))
        assertEquals(Stock.of(10.0), StockRules.validateMinStock(Stock.of(10.0)))
    }
}