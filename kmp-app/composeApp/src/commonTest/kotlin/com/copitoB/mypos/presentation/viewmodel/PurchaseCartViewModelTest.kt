package com.copitoB.mypos.presentation.viewmodel

import com.copitoB.mypos.application.accumulators.UpdateAccumulator
import com.copitoB.mypos.application.backup.TestDb
import com.copitoB.mypos.application.sales.RegisterPurchase
import com.copitoB.mypos.data.repository.SqliteAccumulatorRepository
import com.copitoB.mypos.data.repository.SqliteProductRepository
import com.copitoB.mypos.data.repository.SqliteSaleRepository
import com.copitoB.mypos.data.repository.TpvDatabase
import com.copitoB.mypos.domain.entities.CartItemFactory
import com.copitoB.mypos.domain.valueobjects.Currency
import com.copitoB.mypos.domain.valueobjects.Money
import com.copitoB.mypos.domain.valueobjects.PaymentMethod
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import java.math.BigDecimal
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class PurchaseCartViewModelTest {

  private val driver = TestDb.createDriver()
  private val db = TpvDatabase(driver)
  private val productRepo = SqliteProductRepository(driver)
  private val saleRepo = SqliteSaleRepository(driver)
  private val accumulatorRepo = SqliteAccumulatorRepository(driver)
  private val dispatcher: TestDispatcher = StandardTestDispatcher()

  @AfterTest
  fun tearDown() {
    driver.close()
  }

  private suspend fun seedProduct() {
    val cat = com.copitoB.mypos.data.repository.SqliteCategoryRepository(driver).create("General").id!!
    com.copitoB.mypos.application.catalog.ProductManagement(SqliteProductRepository(driver)).createProduct(
      com.copitoB.mypos.domain.entities.ProductInput(
        categoryId = cat,
        sku = null,
        name = "Item",
        sellingPrice = "10.00",
        purchasePrice = "6.00",
        currentStock = "2.000",
        minStock = "1.000",
        imageUri = null
      )
    )
  }

  @Test
  fun `adds replaces and removes items`() = runTest(dispatcher) {
    seedProduct()
    val vm = PurchaseCartViewModel(
      RegisterPurchase(saleRepo, productRepo, UpdateAccumulator(accumulatorRepo))
    )

    vm.addItem(CartItemFactory.create(productId = 1L, productName = "Item", quantity = 1.0, unitPrice = 6.0))
    assertEquals(1, vm.items.value.size)

    vm.replaceItem(0, CartItemFactory.create(productId = 1L, productName = "Item", quantity = 2.0, unitPrice = 6.0))
    assertEquals(2.0, vm.items.value.first().quantity)

    vm.removeItem(0)
    assertTrue(vm.items.value.isEmpty())
  }

  @Test
  fun `adding the same purchase product twice merges quantity`() = runTest(dispatcher) {
    seedProduct()
    val vm = PurchaseCartViewModel(
      RegisterPurchase(saleRepo, productRepo, UpdateAccumulator(accumulatorRepo))
    )

    vm.addItem(CartItemFactory.create(productId = 1L, productName = "Item", quantity = 1.0, unitPrice = 6.0))
    vm.addItem(CartItemFactory.create(productId = 1L, productName = "Item", quantity = 2.0, unitPrice = 6.0))

    assertEquals(1, vm.items.value.size)
    assertEquals(3.0, vm.items.value.first().quantity)
    assertEquals(null, vm.error.value)
  }

  @Test
  fun `purchase addItem with maxQuantity rejects overflow`() = runTest(dispatcher) {
    seedProduct()
    val vm = PurchaseCartViewModel(
      RegisterPurchase(saleRepo, productRepo, UpdateAccumulator(accumulatorRepo))
    )

    assertTrue(vm.addItem(CartItemFactory.create(productId = 1L, productName = "Item", quantity = 1.0, unitPrice = 6.0), maxQuantity = 2.0))
    assertFalse(vm.addItem(CartItemFactory.create(productId = 1L, productName = "Item", quantity = 2.0, unitPrice = 6.0), maxQuantity = 2.0))
    assertEquals(1.0, vm.items.value.first().quantity)
    assertEquals("stock_error", vm.error.value)
  }

  @Test
  fun `checkout increments stock and clears cart`() = runTest(dispatcher) {
    seedProduct()
    accumulatorRepo.updateAccumulator(Money.zero(Currency.Bs), Money.zero(Currency.Bs), Money.zero(Currency.Bs))
    val vm = PurchaseCartViewModel(
      RegisterPurchase(saleRepo, productRepo, UpdateAccumulator(accumulatorRepo))
    )

    vm.addItem(CartItemFactory.create(productId = 1L, productName = "Item", quantity = 3.0, unitPrice = 6.0))
    var succeeded = false
    vm.checkout(PaymentMethod.CASH) { succeeded = it }
    advanceUntilIdle()
    if (!succeeded) throw AssertionError("checkout failed; error=${vm.error.value}")
    assertTrue(vm.items.value.isEmpty())
    assertEquals(5.0, productRepo.findById(1L)!!.currentStock.value.toDouble())
    assertEquals(1, saleRepo.list("PURCHASE", 10, 0).size)
    assertEquals(3.0, saleRepo.listDetails(1L).sumOf { it.quantity })
  }

  @Test
  fun `checkout clamps cart-level discount`() = runTest(dispatcher) {
    seedProduct()
    val vm = PurchaseCartViewModel(
      RegisterPurchase(saleRepo, productRepo, UpdateAccumulator(accumulatorRepo))
    )
    vm.addItem(CartItemFactory.create(productId = 1L, productName = "Item", quantity = 1.0, unitPrice = 6.0))
    vm.setDiscount("99")
    assertEquals(6.0, vm.discount.value)

    vm.setDiscount("2")
    assertEquals(2.0, vm.discount.value)
  }

  @Test
  fun `empty checkout reports failure`() = runTest(dispatcher) {
    val vm = PurchaseCartViewModel(
      RegisterPurchase(saleRepo, productRepo, UpdateAccumulator(accumulatorRepo))
    )
    var succeeded = true
    vm.checkout(PaymentMethod.CASH) { succeeded = it }
    advanceUntilIdle()
    assertFalse(succeeded)
  }
}