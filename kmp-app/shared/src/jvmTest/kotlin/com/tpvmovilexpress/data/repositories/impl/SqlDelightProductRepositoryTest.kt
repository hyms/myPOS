package com.tpvmovilexpress.data.repositories.impl

import com.squareup.sqldelight.sqlite.driver.JdbcSqliteDriver
import com.tpvmovilexpress.data.database.TPVDatabase
import com.tpvmovilexpress.data.repositories.CreateCategoryInput
import com.tpvmovilexpress.data.repositories.CreateProductInput
import com.tpvmovilexpress.data.repositories.ProductQuery
import com.tpvmovilexpress.data.repositories.SortOrder
import com.tpvmovilexpress.domain.entities.CategoryId
import com.tpvmovilexpress.domain.entities.ProductId
import com.tpvmovilexpress.domain.valueobjects.Currency
import com.tpvmovilexpress.domain.valueobjects.CurrencyCode
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SqlDelightProductRepositoryTest {

    private lateinit var driver: JdbcSqliteDriver
    private lateinit var database: TPVDatabase
    private lateinit var productRepo: SqlDelightProductRepository
    private lateinit var categoryRepo: SqlDelightCategoryRepository
    private val currency = Currency.get(CurrencyCode.BOB)
    private lateinit var defaultCategoryId: CategoryId

    @BeforeTest
    fun setup() {
        driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        TPVDatabase.Schema.create(driver)
        database = TPVDatabase(driver)
        productRepo = SqlDelightProductRepository(database, currency)
        categoryRepo = SqlDelightCategoryRepository(database)

        val category = categoryRepo.create(CreateCategoryInput("Default"))
        defaultCategoryId = category.id
    }

    @AfterTest
    fun teardown() {
        driver.close()
    }

    private fun createProductInput(
        name: String = "Test Product",
        sku: String? = null,
        sellingPrice: Double = 10.0,
        purchasePrice: Double = 5.0,
        currentStock: Double = 100.0,
        minStock: Double = 10.0,
        categoryId: CategoryId = defaultCategoryId
    ) = CreateProductInput(
        categoryId = categoryId,
        sku = sku,
        name = name,
        sellingPrice = sellingPrice,
        purchasePrice = purchasePrice,
        currentStock = currentStock,
        minStock = minStock,
        imageUri = null
    )

    // ─── Create Tests ──────────────────────────

    @Test
    fun `create inserts product with assigned ID`() {
        val product = productRepo.create(createProductInput(name = "Coca-Cola", sku = "COCA-001"))

        assertTrue(product.id.value > 0)
        assertEquals("Coca-Cola", product.name)
        assertEquals("COCA-001", product.sku?.value)
        assertEquals(10.0, product.sellingPrice.amount)
        assertEquals(5.0, product.purchasePrice.amount)
        assertEquals(100.0, product.currentStock.amount)
        assertEquals(10.0, product.minStock.amount)
        assertNull(product.deletedAt)
    }

    @Test
    fun `create throws for empty name`() {
        assertFailsWith<IllegalArgumentException> {
            productRepo.create(createProductInput(name = "   "))
        }
    }

    @Test
    fun `create throws for negative selling price`() {
        assertFailsWith<IllegalArgumentException> {
            productRepo.create(createProductInput(sellingPrice = -1.0))
        }
    }

    @Test
    fun `create throws for negative purchase price`() {
        assertFailsWith<IllegalArgumentException> {
            productRepo.create(createProductInput(purchasePrice = -1.0))
        }
    }

    @Test
    fun `create throws for duplicate SKU`() {
        productRepo.create(createProductInput(name = "Product A", sku = "UNIQUE-001"))

        assertFailsWith<IllegalArgumentException> {
            productRepo.create(createProductInput(name = "Product B", sku = "UNIQUE-001"))
        }
    }

    @Test
    fun `create allows null SKU for multiple products`() {
        val p1 = productRepo.create(createProductInput(name = "No SKU 1"))
        val p2 = productRepo.create(createProductInput(name = "No SKU 2"))

        assertTrue(p1.id.value > 0)
        assertTrue(p2.id.value > 0)
        assertNull(p1.sku)
        assertNull(p2.sku)
    }

    @Test
    fun `create persists fractional stock`() {
        val product = productRepo.create(
            createProductInput(currentStock = 123.456, minStock = 0.001)
        )

        assertEquals(123.456, product.currentStock.amount)
        assertEquals(0.001, product.minStock.amount)
    }

    // ─── FindById Tests ────────────────────────

    @Test
    fun `findById returns existing product`() {
        val created = productRepo.create(createProductInput(name = "Widget"))

        val found = productRepo.findById(created.id)

        assertNotNull(found)
        assertEquals(created.id, found.id)
        assertEquals("Widget", found.name)
    }

    @Test
    fun `findById returns null for non-existent ID`() {
        val found = productRepo.findById(ProductId(999))
        assertNull(found)
    }

    @Test
    fun `findById returns soft-deleted product`() {
        val created = productRepo.create(createProductInput(name = "Deletable"))
        productRepo.delete(created.id)

        val found = productRepo.findById(created.id)

        assertNotNull(found)
        assertNotNull(found.deletedAt)
    }

    // ─── FindBySku Tests ───────────────────────

    @Test
    fun `findBySku returns matching active product`() {
        productRepo.create(createProductInput(name = "SKU Product", sku = "FIND-ME"))

        val found = productRepo.findBySku("FIND-ME")

        assertNotNull(found)
        assertEquals("SKU Product", found.name)
    }

    @Test
    fun `findBySku returns null for non-existent SKU`() {
        val found = productRepo.findBySku("NONEXISTENT")
        assertNull(found)
    }

    @Test
    fun `findBySku returns null for soft-deleted product`() {
        val created = productRepo.create(createProductInput(name = "Deleted SKU", sku = "DEL-SKU"))
        productRepo.delete(created.id)

        val found = productRepo.findBySku("DEL-SKU")
        assertNull(found)
    }

    // ─── Update Tests ──────────────────────────

    @Test
    fun `update modifies product fields`() {
        val created = productRepo.create(createProductInput(name = "Original"))

        val updated = productRepo.update(
            created.id,
            createProductInput(name = "Updated", sellingPrice = 20.0)
        )

        assertEquals("Updated", updated.name)
        assertEquals(20.0, updated.sellingPrice.amount)
    }

    @Test
    fun `update throws for non-existent product`() {
        assertFailsWith<IllegalArgumentException> {
            productRepo.update(ProductId(999), createProductInput())
        }
    }

    @Test
    fun `update throws for duplicate SKU from another product`() {
        productRepo.create(createProductInput(name = "Product A", sku = "SHARED-SKU"))
        val productB = productRepo.create(createProductInput(name = "Product B", sku = "UNIQUE-B"))

        assertFailsWith<IllegalArgumentException> {
            productRepo.update(productB.id, createProductInput(name = "Product B", sku = "SHARED-SKU"))
        }
    }

    @Test
    fun `update allows same SKU on same product`() {
        val created = productRepo.create(createProductInput(name = "Product", sku = "MY-SKU"))

        val updated = productRepo.update(
            created.id,
            createProductInput(name = "Product Updated", sku = "MY-SKU")
        )

        assertEquals("MY-SKU", updated.sku?.value)
    }

    // ─── Soft Delete Tests ─────────────────────

    @Test
    fun `delete sets deleted_at timestamp`() {
        val created = productRepo.create(createProductInput(name = "To Delete"))

        productRepo.delete(created.id)

        val found = productRepo.findById(created.id)
        assertNotNull(found)
        assertNotNull(found.deletedAt)
        assertTrue(found.isDeleted)
    }

    @Test
    fun `deleted product is hidden from list`() {
        val p1 = productRepo.create(createProductInput(name = "Active"))
        val p2 = productRepo.create(createProductInput(name = "To Delete"))

        productRepo.delete(p2.id)

        val list = productRepo.list(ProductQuery())
        assertEquals(1, list.size)
        assertEquals("Active", list[0].product.name)
    }

    @Test
    fun `delete is idempotent on already deleted product`() {
        val created = productRepo.create(createProductInput(name = "Double Delete"))

        productRepo.delete(created.id)
        productRepo.delete(created.id)

        val found = productRepo.findById(created.id)
        assertNotNull(found)
        assertNotNull(found.deletedAt)
    }

    // ─── List & Query Tests ────────────────────

    @Test
    fun `list returns active products with default sort`() {
        productRepo.create(createProductInput(name = "Banana"))
        productRepo.create(createProductInput(name = "Apple"))
        productRepo.create(createProductInput(name = "Cherry"))

        val results = productRepo.list(ProductQuery())

        assertEquals(3, results.size)
        assertEquals("Apple", results[0].product.name)
        assertEquals("Banana", results[1].product.name)
        assertEquals("Cherry", results[2].product.name)
    }

    @Test
    fun `list respects search filter`() {
        productRepo.create(createProductInput(name = "Coca-Cola"))
        productRepo.create(createProductInput(name = "Pepsi"))
        productRepo.create(createProductInput(name = "Coca-Light"))

        val results = productRepo.list(ProductQuery(search = "Coca"))

        assertEquals(2, results.size)
    }

    @Test
    fun `list respects category filter`() {
        val beverages = categoryRepo.create(CreateCategoryInput("Beverages"))
        val snacks = categoryRepo.create(CreateCategoryInput("Snacks"))

        productRepo.create(createProductInput(name = "Soda", categoryId = beverages.id))
        productRepo.create(createProductInput(name = "Chips", categoryId = snacks.id))
        productRepo.create(createProductInput(name = "Water", categoryId = beverages.id))

        val results = productRepo.list(ProductQuery(categoryId = beverages.id))

        assertEquals(2, results.size)
        assertTrue(results.all { it.product.categoryId == beverages.id })
    }

    @Test
    fun `list respects pagination`() {
        for (i in 1..10) {
            productRepo.create(createProductInput(name = "Product ${i.toString().padStart(2, '0')}"))
        }

        val page1 = productRepo.list(ProductQuery(limit = 3, offset = 0))
        val page2 = productRepo.list(ProductQuery(limit = 3, offset = 3))

        assertEquals(3, page1.size)
        assertEquals(3, page2.size)
        assertTrue(page1[0].product.name != page2[0].product.name)
    }

    @Test
    fun `list returns popularity score`() {
        val product = productRepo.create(createProductInput(name = "Popular Item"))

        val results = productRepo.list(ProductQuery())

        assertEquals(1, results.size)
        assertEquals(0, results[0].popularity)
    }

    // ─── FindByIds Tests ───────────────────────

    @Test
    fun `findByIds returns matching products`() {
        val p1 = productRepo.create(createProductInput(name = "Product A"))
        val p2 = productRepo.create(createProductInput(name = "Product B"))
        productRepo.create(createProductInput(name = "Product C"))

        val found = productRepo.findByIds(listOf(p1.id, p2.id))

        assertEquals(2, found.size)
    }

    @Test
    fun `findByIds returns empty for empty input`() {
        val found = productRepo.findByIds(emptyList())
        assertTrue(found.isEmpty())
    }

    @Test
    fun `findByIds excludes soft-deleted products`() {
        val p1 = productRepo.create(createProductInput(name = "Active"))
        val p2 = productRepo.create(createProductInput(name = "Deleted"))
        productRepo.delete(p2.id)

        val found = productRepo.findByIds(listOf(p1.id, p2.id))

        assertEquals(1, found.size)
        assertEquals("Active", found[0].name)
    }
}
