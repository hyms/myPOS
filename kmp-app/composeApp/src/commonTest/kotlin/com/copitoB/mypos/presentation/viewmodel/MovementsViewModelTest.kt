package com.copitoB.mypos.presentation.viewmodel

import com.copitoB.mypos.application.accumulators.UpdateAccumulator
import com.copitoB.mypos.application.backup.TestDb
import com.copitoB.mypos.application.transactions.ListMovements
import com.copitoB.mypos.application.transactions.ReverseTransaction
import com.copitoB.mypos.application.transactions.RevertTransaction
import com.copitoB.mypos.data.repository.SqliteAccumulatorRepository
import com.copitoB.mypos.data.repository.SqliteProductRepository
import com.copitoB.mypos.data.repository.SqliteSaleRepository
import com.copitoB.mypos.data.repository.TpvDatabase
import com.copitoB.mypos.domain.valueobjects.Currency
import com.copitoB.mypos.domain.valueobjects.Money
import com.copitoB.mypos.domain.valueobjects.PaymentMethod
import com.copitoB.mypos.domain.valueobjects.TransactionType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import java.math.BigDecimal
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class MovementsViewModelTest {

  private val driver = TestDb.createDriver()
  private val db = TpvDatabase(driver)
  private val saleRepo = SqliteSaleRepository(driver)
  private val productRepo = SqliteProductRepository(driver)
  private val accumulatorRepo = SqliteAccumulatorRepository(driver)
  private val dispatcher = StandardTestDispatcher()

  private val listMovements = ListMovements(saleRepo)
  private val revertTransaction = RevertTransaction(saleRepo, productRepo, UpdateAccumulator(accumulatorRepo))
  private val reverseTransaction = ReverseTransaction(saleRepo, revertTransaction)

  @BeforeTest
  fun setup() {
    Dispatchers.setMain(dispatcher)
  }

  @AfterTest
  fun tearDown() {
    Dispatchers.resetMain()
    driver.close()
  }

  private fun money(value: String) = Money(BigDecimal(value), Currency.Bs)

  private fun viewModel() = MovementsViewModel(listMovements, reverseTransaction, saleRepo, productRepo)

  @Test
  fun `filter narrows loaded movements`() = runTest(dispatcher) {
    saleRepo.create("SALE", money("10.00"), PaymentMethod.CASH, "a")
    saleRepo.create("PURCHASE", money("20.00"), PaymentMethod.CASH, "b")
    saleRepo.create("EXPENSE", money("5.00"), PaymentMethod.CASH, "c")

    val vm = viewModel()
    vm.load()
    advanceUntilIdle()
    assertEquals(3, vm.movements.value.size)

    vm.setFilter(TransactionType.SALE)
    advanceUntilIdle()
    assertEquals(1, vm.movements.value.size)
    assertEquals("SALE", vm.movements.value.first().type)
  }

  @Test
  fun `search narrows by detail`() = runTest(dispatcher) {
    saleRepo.create("SALE", money("10.00"), PaymentMethod.CASH, "coffee")
    saleRepo.create("SALE", money("20.00"), PaymentMethod.CASH, "tea")

    val vm = viewModel()
    vm.setSearch("coffee")
    advanceUntilIdle()

    assertEquals(1, vm.movements.value.size)
    assertEquals("coffee", vm.movements.value.first().detail)
  }

  @Test
  fun `request revert requires pin when configured`() = runTest(dispatcher) {
    saleRepo.create("SALE", money("10.00"), PaymentMethod.CASH, "a")
    val vm = viewModel()
    vm.load()
    advanceUntilIdle()

    vm.openDetail(vm.movements.value.first())
    advanceUntilIdle()
    vm.requestRevert(hasPin = true)
    assertEquals(RevertState.AwaitingPin, vm.revertState.value)

    vm.cancelRevert()
    assertEquals(RevertState.Idle, vm.revertState.value)
  }

  @Test
  fun `confirm revert without pin restores state`() = runTest(dispatcher) {
    accumulatorRepo.updateAccumulator(money("10.00"), Money.zero(), Money.zero())
    val sale = saleRepo.create("SALE", money("10.00"), PaymentMethod.CASH, "a")
    val vm = viewModel()
    vm.load()
    advanceUntilIdle()

    vm.openDetail(vm.movements.value.first())
    advanceUntilIdle()
    vm.requestRevert(hasPin = false)
    assertEquals(RevertState.Confirming, vm.revertState.value)

    var done = false
    vm.confirmRevert { done = it }
    advanceUntilIdle()

    assertTrue(done)
    assertEquals(null, vm.selected.value)
    assertEquals(money("0.00"), accumulatorRepo.getCashSummary().totalSales)
  }
}