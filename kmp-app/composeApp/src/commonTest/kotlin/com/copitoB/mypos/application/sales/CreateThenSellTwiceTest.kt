package com.copitoB.mypos.application.sales

import com.copitoB.mypos.application.accumulators.UpdateAccumulator
import com.copitoB.mypos.application.backup.TestDb
import com.copitoB.mypos.application.catalog.ProductManagement
import com.copitoB.mypos.data.repository.SqliteAccumulatorRepository
import com.copitoB.mypos.data.repository.SqliteCategoryRepository
import com.copitoB.mypos.data.repository.SqliteProductRepository
import com.copitoB.mypos.data.repository.SqliteSaleRepository
import com.copitoB.mypos.data.repository.TpvDatabase
import com.copitoB.mypos.domain.entities.ICartItem
import com.copitoB.mypos.domain.entities.ProductInput
import com.copitoB.mypos.domain.entities.SimpleCartItem
import com.copitoB.mypos.domain.valueobjects.Currency
import com.copitoB.mypos.domain.valueobjects.Money
import com.copitoB.mypos.domain.valueobjects.PaymentMethod
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CreateThenSellTwiceTest {

  private val driver = TestDb.createDriver()
  private val db = TpvDatabase(driver)
  private val productRepo = SqliteProductRepository(driver)
  private val saleRepo = SqliteSaleRepository(driver)
  private val accumulatorRepo = SqliteAccumulatorRepository(driver)
  private val management = ProductManagement(productRepo)

  @AfterTest
  fun tearDown() {
    driver.close()
  }

  private suspend fun registerSale(): RegisterSale {
    accumulatorRepo.updateAccumulator(Money.zero(Currency.Bs), Money.zero(Currency.Bs), Money.zero(Currency.Bs))
    return RegisterSale(saleRepo, productRepo, UpdateAccumulator(accumulatorRepo))
  }

  private fun item(id: Long, quantity: Double): ICartItem =
    SimpleCartItem(productId = id, productName = "Widget", quantity = quantity, unitPrice = 10.0)

  @Test
  fun `two consecutive sales after creating a product both succeed`() {
    kotlinx.coroutines.runBlocking {
      val category = SqliteCategoryRepository(driver).create("General").id!!
      val product = management.createProduct(
        ProductInput(
          categoryId = category,
          sku = null,
          name = "Widget",
          sellingPrice = "10.00",
          purchasePrice = "5.00",
          currentStock = "2.000",
          minStock = "1.000",
          imageUri = null
        )
      )
      val id = product.id!!

      registerSale().execute(items = listOf(item(id, 1.0)), paymentMethod = PaymentMethod.CASH, received = null)
      assertEquals(1.0, productRepo.findById(id)!!.currentStock.value.toDouble())

      registerSale().execute(items = listOf(item(id, 1.0)), paymentMethod = PaymentMethod.CASH, received = null)
      val remaining = productRepo.findById(id)!!.currentStock
      assertEquals(0.0, remaining.value.toDouble())
      assertTrue(remaining.isZero())
      assertEquals(2, saleRepo.list("SALE", 10, 0).size)
    }
  }
}