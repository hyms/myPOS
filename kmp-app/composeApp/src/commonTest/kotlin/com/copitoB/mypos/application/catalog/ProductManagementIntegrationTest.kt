package com.copitoB.mypos.application.catalog

import com.copitoB.mypos.application.backup.TestDb
import com.copitoB.mypos.data.repository.SqliteProductRepository
import com.copitoB.mypos.data.repository.TpvDatabase
import com.copitoB.mypos.domain.entities.ProductInput
import com.copitoB.mypos.domain.valueobjects.Stock
import java.math.BigDecimal
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ProductManagementIntegrationTest {

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

  private fun input(name: String, sku: String? = null, stock: String = "10.000") = ProductInput(
    categoryId = 1L,
    sku = sku,
    name = name,
    sellingPrice = "10.00",
    purchasePrice = "5.00",
    currentStock = stock,
    minStock = "2.000",
    imageUri = null
  )

  @Test
  fun `creates product and rejects duplicate sku`() {
    kotlinx.coroutines.runBlocking {
      seedCategory()
      management.createProduct(input("Widget", sku = "W-1"))
      val error = assertFailsWith<IllegalArgumentException> {
        management.createProduct(input("Other", sku = "W-1"))
      }
      assertTrue(error.message!!.contains("SKU already exists"))
    }
  }

  @Test
  fun `updates product`() {
    kotlinx.coroutines.runBlocking {
      seedCategory()
      val created = management.createProduct(input("Widget"))
      management.updateProduct(
        created.id!!,
        input("Widget v2", stock = "25.000").copy(categoryId = created.categoryId)
      )
      val reloaded = management.getProduct(created.id!!)!!
      assertEquals("Widget v2", reloaded.name)
      assertEquals(25.0, reloaded.currentStock.value.toDouble())
      assertNotNull(reloaded.updatedAt)
    }
  }

  @Test
  fun `soft delete hides product from reads`() {
    kotlinx.coroutines.runBlocking {
      seedCategory()
      val created = management.createProduct(input("Widget"))
      management.deleteProduct(created.id!!)
      assertNull(management.getProduct(created.id!!))
      assertTrue(management.listProducts().none { it.id == created.id })
    }
  }

  @Test
  fun `adjust stock applies delta to current`() {
    kotlinx.coroutines.runBlocking {
      seedCategory()
      val created = management.createProduct(input("Widget", stock = "10.000"))
      management.adjustStock(created.id!!, Stock(BigDecimal("4.500")), "Stock count")
      assertEquals(14.5, management.getProduct(created.id!!)!!.currentStock.value.toDouble())
    }
  }

  @Test
  fun `getLowStockProducts returns below-min products`() {
    kotlinx.coroutines.runBlocking {
      seedCategory()
      management.createProduct(input("Low", stock = "1.000"))
      management.createProduct(input("Ok", stock = "50.000"))
      val low = management.getLowStockProducts()
      assertEquals(1, low.size)
      assertEquals("Low", low.first().name)
    }
  }

  @Test
  fun `Stock parse returns null on garbage`() {
    assertNull(Stock.parseOrNull("abc"))
    assertNotNull(Stock.parseOrNull("1.5"))
  }
}