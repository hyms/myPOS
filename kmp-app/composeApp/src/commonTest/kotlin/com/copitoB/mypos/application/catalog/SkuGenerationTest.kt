package com.copitoB.mypos.application.catalog

import com.copitoB.mypos.application.backup.TestDb
import com.copitoB.mypos.data.repository.SqliteProductRepository
import com.copitoB.mypos.data.repository.TpvDatabase
import com.copitoB.mypos.domain.entities.ProductInput
import java.math.BigDecimal
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class SkuGenerationTest {

  private val driver = TestDb.createDriver()
  private val db = TpvDatabase(driver)
  private val repository = SqliteProductRepository(driver)
  private val management = ProductManagement(repository)

  @AfterTest
  fun tearDown() {
    driver.close()
  }

  private suspend fun seedCategory(): Long {
    val category = com.copitoB.mypos.data.repository.SqliteCategoryRepository(driver).create("General")
    return category.id!!
  }

  private fun input(name: String, sku: String?) = ProductInput(
    categoryId = 1L,
    sku = sku,
    name = name,
    sellingPrice = "10.00",
    purchasePrice = "5.00",
    currentStock = "10.000",
    minStock = "2.000",
    imageUri = null
  )

  @Test
  fun `generateUniqueSku starts at PRD-0001`() {
    kotlinx.coroutines.runBlocking {
      seedCategory()
      assertEquals("PRD-0001", management.generateUniqueSku())
    }
  }

  @Test
  fun `generateUniqueSku skips existing sku`() {
    kotlinx.coroutines.runBlocking {
      seedCategory()
      management.createProduct(input("First", sku = "PRD-0001"))
      assertEquals("PRD-0002", management.generateUniqueSku())
    }
  }

  @Test
  fun `generated sku is accepted by SKU rule`() {
    kotlinx.coroutines.runBlocking {
      seedCategory()
      val sku = management.generateUniqueSku()
      com.copitoB.mypos.domain.valueobjects.SKU.create(sku)
      assertEquals(sku.length, 8)
    }
  }

  @Test
  fun `adjust stock rejects negative absolute value`() {
    kotlinx.coroutines.runBlocking {
      seedCategory()
      val created = management.createProduct(input("Widget", sku = null))
      assertFailsWith<IllegalArgumentException> {
        management.adjustStock(
          created.id!!,
          com.copitoB.mypos.domain.valueobjects.Stock(BigDecimal("-1")),
          "bad"
        )
      }
      assertTrue(management.getProduct(created.id!!)!!.currentStock.value.signum() >= 0)
    }
  }
}