package com.tpvmovilexpress.data.repositories.impl

import com.squareup.sqldelight.sqlite.driver.JdbcSqliteDriver
import com.tpvmovilexpress.data.database.TPVDatabase
import com.tpvmovilexpress.data.repositories.CreateCategoryInput
import com.tpvmovilexpress.data.repositories.CreateProductInput
import com.tpvmovilexpress.domain.entities.ProductId
import com.tpvmovilexpress.domain.entities.StockMovementId
import com.tpvmovilexpress.domain.entities.StockMovementType
import com.tpvmovilexpress.domain.valueobjects.Currency
import com.tpvmovilexpress.domain.valueobjects.CurrencyCode
import com.tpvmovilexpress.domain.valueobjects.Stock
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SqlDelightStockMovementRepositoryTest {

    private lateinit var driver: JdbcSqliteDriver
    private lateinit var database: TPVDatabase
    private lateinit var stockMovementRepo: SqlDelightStockMovementRepository
    private lateinit var productRepo: SqlDelightProductRepository
    private lateinit var categoryRepo: SqlDelightCategoryRepository
    private val currency = Currency.get(CurrencyCode.BOB)
    private lateinit var testProductId: ProductId

    @BeforeTest
    fun setup() {
        driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        TPVDatabase.Schema.create(driver)
        database = TPVDatabase(driver)
        stockMovementRepo = SqlDelightStockMovementRepository(database)
        productRepo = SqlDelightProductRepository(database, currency)
        categoryRepo = SqlDelightCategoryRepository(database)

        val category = categoryRepo.create(CreateCategoryInput("Test"))
        val product = productRepo.create(
            CreateProductInput(
                categoryId = category.id,
                sku = null,
                name = "Test Product",
                sellingPrice = 10.0,
                purchasePrice = 5.0,
                currentStock = 100.0,
                minStock = 10.0,
                imageUri = null
            )
        )
        testProductId = product.id
    }

    @AfterTest
    fun teardown() {
        driver.close()
    }

    @Test
    fun `create inserts IN movement`() {
        val movement = stockMovementRepo.create(
            productId = testProductId,
            type = StockMovementType.IN,
            quantity = Stock.of(50.0),
            sourceTransactionId = 1L,
            note = "Purchase received"
        )

        assertTrue(movement.id.value > 0)
        assertEquals(testProductId, movement.productId)
        assertEquals(StockMovementType.IN, movement.type)
        assertEquals(50.0, movement.quantity.amount)
        assertNotNull(movement.sourceTransactionId)
        assertEquals("Purchase received", movement.note)
    }

    @Test
    fun `create inserts OUT movement`() {
        val movement = stockMovementRepo.create(
            productId = testProductId,
            type = StockMovementType.OUT,
            quantity = Stock.of(25.5)
        )

        assertEquals(StockMovementType.OUT, movement.type)
        assertEquals(25.5, movement.quantity.amount)
        assertNull(movement.sourceTransactionId)
        assertNull(movement.note)
    }

    @Test
    fun `create inserts ADJUSTMENT movement`() {
        val movement = stockMovementRepo.create(
            productId = testProductId,
            type = StockMovementType.ADJUSTMENT,
            quantity = Stock.of(5.0),
            note = "Inventory correction"
        )

        assertEquals(StockMovementType.ADJUSTMENT, movement.type)
        assertEquals("Inventory correction", movement.note)
    }

    @Test
    fun `listByProductId returns movements newest first`() {
        stockMovementRepo.create(
            productId = testProductId,
            type = StockMovementType.IN,
            quantity = Stock.of(100.0),
            note = "First"
        )
        stockMovementRepo.create(
            productId = testProductId,
            type = StockMovementType.OUT,
            quantity = Stock.of(20.0),
            note = "Second"
        )
        stockMovementRepo.create(
            productId = testProductId,
            type = StockMovementType.OUT,
            quantity = Stock.of(10.0),
            note = "Third"
        )

        val movements = stockMovementRepo.listByProductId(testProductId)

        assertEquals(3, movements.size)
        assertEquals("Third", movements[0].note)
        assertEquals("Second", movements[1].note)
        assertEquals("First", movements[2].note)
    }

    @Test
    fun `listByProductId respects pagination`() {
        for (i in 1..10) {
            stockMovementRepo.create(
                productId = testProductId,
                type = StockMovementType.IN,
                quantity = Stock.of(i.toDouble()),
                note = "Movement $i"
            )
        }

        val page = stockMovementRepo.listByProductId(testProductId, limit = 3, offset = 0)

        assertEquals(3, page.size)
    }

    @Test
    fun `findById returns existing movement`() {
        val created = stockMovementRepo.create(
            productId = testProductId,
            type = StockMovementType.IN,
            quantity = Stock.of(50.0)
        )

        val found = stockMovementRepo.findById(created.id)

        assertNotNull(found)
        assertEquals(created.id, found.id)
        assertEquals(50.0, found.quantity.amount)
    }

    @Test
    fun `findById returns null for non-existent ID`() {
        val found = stockMovementRepo.findById(StockMovementId(999))
        assertNull(found)
    }

    @Test
    fun `stock movements are append-only audit trail`() {
        stockMovementRepo.create(
            productId = testProductId,
            type = StockMovementType.IN,
            quantity = Stock.of(100.0)
        )
        stockMovementRepo.create(
            productId = testProductId,
            type = StockMovementType.OUT,
            quantity = Stock.of(30.0)
        )

        val movements = stockMovementRepo.listByProductId(testProductId)
        assertEquals(2, movements.size)
    }
}
