package com.copitoB.mypos.application.catalog

import com.copitoB.mypos.application.backup.TestDb
import com.copitoB.mypos.data.repository.SqliteProductRepository
import com.copitoB.mypos.data.repository.SqliteSaleRepository
import com.copitoB.mypos.data.repository.TpvDatabase
import com.copitoB.mypos.domain.entities.ProductInput
import com.copitoB.mypos.domain.entities.ProductSort
import com.copitoB.mypos.domain.valueobjects.Currency
import com.copitoB.mypos.domain.valueobjects.Money
import com.copitoB.mypos.domain.valueobjects.PaymentMethod
import java.math.BigDecimal
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ProductSortQueryTest {

  private val driver = TestDb.createDriver()
  private val db = TpvDatabase(driver)
  private val productRepo = SqliteProductRepository(driver)
  private val saleRepo = SqliteSaleRepository(driver)
  private val management = ProductManagement(productRepo)

  @AfterTest
  fun tearDown() {
    driver.close()
  }

  private suspend fun seed() {
    val cat = com.copitoB.mypos.data.repository.SqliteCategoryRepository(driver).create("General").id!!
    val products = listOf(
      Triple("Bebida", "10.00", "5.000"),
      Triple("Pan", "20.00", "8.000"),
      Triple("Carne", "5.00", "0.500")
    )
    products.forEach { (name, price, stock) ->
      management.createProduct(
        ProductInput(
          categoryId = cat,
          sku = null,
          name = name,
          sellingPrice = price,
          purchasePrice = "1.00",
          currentStock = stock,
          minStock = "1.000",
          imageUri = null
        )
      )
    }
  }

  @Test
  fun `popularity sort orders by sold quantity`() {
    kotlinx.coroutines.runBlocking { seed() }
    kotlinx.coroutines.runBlocking {
      val sale = saleRepo.create("SALE", Money(BigDecimal("100"), Currency.Bs), PaymentMethod.CASH, null)
      saleRepo.addDetail(sale.id!!, 1L, 5.0, 10.0)
      saleRepo.addDetail(sale.id!!, 2L, 1.0, 20.0)

      val popular = management.listProducts(sort = ProductSort.POPULARIDAD_DESC)
      assertEquals(listOf("Bebida", "Pan", "Carne"), popular.map { it.name })

      val byName = management.listProducts(sort = ProductSort.NOMBRE_ASC)
      assertEquals(listOf("Bebida", "Carne", "Pan"), byName.map { it.name })
    }
  }

  @Test
  fun `low-stock filter returns only products below minimum`() {
    kotlinx.coroutines.runBlocking { seed() }
    kotlinx.coroutines.runBlocking {
      val low = management.listProducts(lowStockOnly = true)
      assertEquals(listOf("Carne"), low.map { it.name })
      val all = management.listProducts()
      assertEquals(3, all.size)
    }
  }

  @Test
  fun `purchases are excluded from popularity`() {
    kotlinx.coroutines.runBlocking { seed() }
    kotlinx.coroutines.runBlocking {
      val purchase = saleRepo.create("PURCHASE", Money(BigDecimal("50"), Currency.Bs), PaymentMethod.CASH, null)
      saleRepo.addDetail(purchase.id!!, 3L, 100.0, 5.0)

      val popular = management.listProducts(sort = ProductSort.POPULARIDAD_DESC)
      assertEquals(listOf("Carne", "Bebida", "Pan"), popular.map { it.name })
    }
  }

  @Test
  fun `pagination splits results`() {
    kotlinx.coroutines.runBlocking { seed() }
    kotlinx.coroutines.runBlocking {
      val first = management.listProducts(limit = 2, offset = 0)
      val second = management.listProducts(limit = 2, offset = 2)
      assertEquals(2, first.size)
      assertEquals(1, second.size)
      assertTrue(second.none { it.id == first.first().id })
    }
  }

  @Test
  fun `search filters by name`() {
    kotlinx.coroutines.runBlocking { seed() }
    kotlinx.coroutines.runBlocking {
      val results = management.listProducts(search = "Pan")
      assertEquals(1, results.size)
      assertEquals("Pan", results.first().name)
      assertNull(management.listProducts(search = "zzz").firstOrNull())
    }
  }
}