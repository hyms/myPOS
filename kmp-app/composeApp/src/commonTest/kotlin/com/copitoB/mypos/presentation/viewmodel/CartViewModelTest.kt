package com.copitoB.mypos.presentation.viewmodel

import com.copitoB.mypos.application.accumulators.UpdateAccumulator
import com.copitoB.mypos.application.backup.TestDb
import com.copitoB.mypos.application.sales.RegisterSale
import com.copitoB.mypos.data.repository.SqliteAccumulatorRepository
import com.copitoB.mypos.data.repository.SqliteProductRepository
import com.copitoB.mypos.data.repository.SqliteSaleRepository
import com.copitoB.mypos.data.repository.TpvDatabase
import com.copitoB.mypos.domain.entities.CartItemFactory
import com.copitoB.mypos.domain.valueobjects.Currency
import com.copitoB.mypos.domain.valueobjects.Money
import java.math.BigDecimal
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CartViewModelTest {

  private val driver = TestDb.createDriver()
  private val db = TpvDatabase(driver)
  private val productRepo = SqliteProductRepository(driver)
  private val saleRepo = SqliteSaleRepository(driver)
  private val accumulatorRepo = SqliteAccumulatorRepository(driver)

  @AfterTest
  fun tearDown() {
    driver.close()
  }

  private fun viewModel(): CartViewModel {
    kotlinx.coroutines.runBlocking {
      accumulatorRepo.updateAccumulator(Money.zero(Currency.Bs), Money.zero(Currency.Bs), Money.zero(Currency.Bs))
    }
    return CartViewModel(RegisterSale(saleRepo, productRepo, UpdateAccumulator(accumulatorRepo)))
  }

  private fun item(id: Long, quantity: Double = 1.0) =
    CartItemFactory.create(productId = id, productName = "P$id", quantity = quantity, unitPrice = 10.0)

  @Test
  fun `addItem same product twice merges quantity`() {
    val vm = viewModel()
    assertTrue(vm.addItem(item(1L)))
    assertTrue(vm.addItem(item(1L)))
    assertEquals(1, vm.items.value.size)
    assertEquals(2.0, vm.items.value.first().quantity)
    assertEquals(null, vm.error.value)
  }

  @Test
  fun `addItem beyond maxQuantity is rejected`() {
    val vm = viewModel()
    assertTrue(vm.addItem(item(1L), maxQuantity = 1.0))
    assertFalse(vm.addItem(item(1L), maxQuantity = 1.0))
    assertEquals(1, vm.items.value.size)
    assertEquals(1.0, vm.items.value.first().quantity)
    assertEquals("stock_error", vm.error.value)
  }

  @Test
  fun `addItem within maxQuantity is accepted`() {
    val vm = viewModel()
    assertTrue(vm.addItem(item(1L), maxQuantity = 5.0))
    assertTrue(vm.addItem(item(1L), maxQuantity = 5.0))
    assertEquals(2.0, vm.items.value.first().quantity)
    assertEquals(null, vm.error.value)
  }

  @Test
  fun `addItem different products keeps both rows`() {
    val vm = viewModel()
    assertTrue(vm.addItem(item(1L)))
    assertTrue(vm.addItem(item(2L)))
    assertEquals(2, vm.items.value.size)
  }

  @Test
  fun `addItem without maxQuantity never rejects`() {
    val vm = viewModel()
    assertTrue(vm.addItem(item(1L)))
    assertTrue(vm.addItem(item(1L)))
    assertTrue(vm.addItem(item(1L)))
    assertEquals(3.0, vm.items.value.first().quantity)
  }

  @Test
  fun `removeItem targets the requested index after merge`() {
    val vm = viewModel()
    vm.addItem(item(1L))
    vm.addItem(item(2L))
    vm.addItem(item(2L))
    assertEquals(2, vm.items.value.size)
    vm.removeItem(1)
    assertEquals(1, vm.items.value.size)
    assertEquals(1L, vm.items.value.first().productId)
  }
}