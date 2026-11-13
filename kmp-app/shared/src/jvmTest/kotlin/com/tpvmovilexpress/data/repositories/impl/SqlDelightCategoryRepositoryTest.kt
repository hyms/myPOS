package com.tpvmovilexpress.data.repositories.impl

import com.squareup.sqldelight.sqlite.driver.JdbcSqliteDriver
import com.tpvmovilexpress.data.database.TPVDatabase
import com.tpvmovilexpress.data.repositories.CreateCategoryInput
import com.tpvmovilexpress.domain.entities.CategoryId
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SqlDelightCategoryRepositoryTest {

    private lateinit var driver: JdbcSqliteDriver
    private lateinit var database: TPVDatabase
    private lateinit var repository: SqlDelightCategoryRepository

    @BeforeTest
    fun setup() {
        driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        TPVDatabase.Schema.create(driver)
        database = TPVDatabase(driver)
        repository = SqlDelightCategoryRepository(database)
    }

    @AfterTest
    fun teardown() {
        driver.close()
    }

    @Test
    fun `create inserts category and returns with assigned ID`() {
        val category = repository.create(CreateCategoryInput("Bebidas"))

        assertTrue(category.id.value > 0)
        assertEquals("Bebidas", category.name)
        assertNull(category.deletedAt)
    }

    @Test
    fun `create throws for empty name`() {
        assertFailsWith<IllegalArgumentException> {
            repository.create(CreateCategoryInput("   "))
        }
    }

    @Test
    fun `list returns all active categories sorted by name`() {
        repository.create(CreateCategoryInput("Zumos"))
        repository.create(CreateCategoryInput("Bebidas"))
        repository.create(CreateCategoryInput("Alimentos"))

        val categories = repository.list()

        assertEquals(3, categories.size)
        assertEquals("Alimentos", categories[0].name)
        assertEquals("Bebidas", categories[1].name)
        assertEquals("Zumos", categories[2].name)
    }

    @Test
    fun `list returns empty for no categories`() {
        val categories = repository.list()
        assertTrue(categories.isEmpty())
    }

    @Test
    fun `findById returns existing category`() {
        val created = repository.create(CreateCategoryInput("Bebidas"))

        val found = repository.findById(created.id)

        assertNotNull(found)
        assertEquals(created.id, found.id)
        assertEquals("Bebidas", found.name)
    }

    @Test
    fun `findById returns null for non-existent ID`() {
        val found = repository.findById(CategoryId(999))
        assertNull(found)
    }

    @Test
    fun `create preserves creation timestamp`() {
        val category = repository.create(CreateCategoryInput("Test"))

        assertTrue(category.registeredAt.isNotEmpty())
        assertTrue(category.registeredAt.contains("T"))
    }
}
