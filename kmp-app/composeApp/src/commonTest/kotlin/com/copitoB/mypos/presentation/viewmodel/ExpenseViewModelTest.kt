package com.copitoB.mypos.presentation.viewmodel

import com.copitoB.mypos.application.accumulators.UpdateAccumulator
import com.copitoB.mypos.application.backup.TestDb
import com.copitoB.mypos.application.expenses.RegisterExpense
import com.copitoB.mypos.data.repository.SqliteAccumulatorRepository
import com.copitoB.mypos.data.repository.SqliteExpenseRepository
import com.copitoB.mypos.data.repository.TpvDatabase
import com.copitoB.mypos.domain.valueobjects.PaymentMethod
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class ExpenseViewModelTest {

  private val driver = TestDb.createDriver()
  private val db = TpvDatabase(driver)
  private val expenseRepo = SqliteExpenseRepository(driver)
  private val accumulatorRepo = SqliteAccumulatorRepository(driver)
  private val dispatcher = StandardTestDispatcher()

  @BeforeTest
  fun setup() {
    Dispatchers.setMain(dispatcher)
  }

  @AfterTest
  fun tearDown() {
    Dispatchers.resetMain()
    driver.close()
  }

  private suspend fun viewModel(): ExpenseViewModel {
    accumulatorRepo.updateAccumulator(
      com.copitoB.mypos.domain.valueobjects.Money.zero(),
      com.copitoB.mypos.domain.valueobjects.Money.zero(),
      com.copitoB.mypos.domain.valueobjects.Money.zero()
    )
    return ExpenseViewModel(expenseRepo, RegisterExpense(expenseRepo, UpdateAccumulator(accumulatorRepo)))
  }

  @Test
  fun `register blocked when amount invalid`() = runTest(dispatcher) {
    val vm = viewModel()
    val category = expenseRepo.createCategory("Cat")
    vm.setCategory(category.id)
    vm.setAmount("0")
    vm.setDescription("desc")

    vm.registerExpense { }
    advanceUntilIdle()

    assertEquals(ExpenseFormError.Amount, vm.formError.value)
    assertTrue(vm.expenses.value.isEmpty())
  }

  @Test
  fun `register blocked when category missing`() = runTest(dispatcher) {
    val vm = viewModel()
    vm.setAmount("10.00")
    vm.setDescription("desc")

    vm.registerExpense { }
    advanceUntilIdle()

    assertEquals(ExpenseFormError.Category, vm.formError.value)
  }

  @Test
  fun `successful registration persists expense and clears form`() = runTest(dispatcher) {
    val vm = viewModel()
    val category = expenseRepo.createCategory("Cat")
    vm.loadExpenses()
    advanceUntilIdle()
    vm.setCategory(category.id)
    vm.setAmount("12.50")
    vm.setDescription("lunch")
    vm.setPaymentMethod(PaymentMethod.CARD)

    var done = false
    vm.registerExpense { done = it }
    advanceUntilIdle()

    assertTrue(done)
    assertEquals("", vm.amountText.value)
    assertEquals(1, expenseRepo.list(10, 0).size)
    assertEquals(PaymentMethod.CARD, expenseRepo.list(10, 0).first().paymentMethod)
  }

  @Test
  fun `pagination appends and toggles hasMore`() = runTest(dispatcher) {
    val vm = viewModel()
    val category = expenseRepo.createCategory("Cat").id!!
    val acc = UpdateAccumulator(accumulatorRepo)
    repeat(5) {
      RegisterExpense(expenseRepo, acc).execute(
        categoryId = category,
        amount = com.copitoB.mypos.domain.valueobjects.Money(java.math.BigDecimal("$it.01"), com.copitoB.mypos.domain.valueobjects.Currency.Bs),
        description = "e$it",
        paymentMethod = PaymentMethod.CASH
      )
    }

    vm.setPageSize(2)
    vm.loadExpenses()
    advanceUntilIdle()
    assertEquals(2, vm.expenses.value.size)
    assertTrue(vm.hasMore.value)

    vm.loadNextPage()
    advanceUntilIdle()
    assertEquals(4, vm.expenses.value.size)
  }
}