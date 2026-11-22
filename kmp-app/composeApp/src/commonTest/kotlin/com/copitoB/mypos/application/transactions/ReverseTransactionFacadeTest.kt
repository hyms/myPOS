package com.copitoB.mypos.application.transactions

import com.copitoB.mypos.application.accumulators.UpdateAccumulator
import com.copitoB.mypos.application.backup.TestDb
import com.copitoB.mypos.data.repository.SqliteAccumulatorRepository
import com.copitoB.mypos.data.repository.SqliteCategoryRepository
import com.copitoB.mypos.data.repository.SqliteExpenseRepository
import com.copitoB.mypos.data.repository.SqliteProductRepository
import com.copitoB.mypos.data.repository.SqliteSaleRepository
import com.copitoB.mypos.data.repository.TpvDatabase
import com.copitoB.mypos.domain.entities.ExpenseInput
import com.copitoB.mypos.domain.entities.ProductInput
import com.copitoB.mypos.domain.errors.StockError
import com.copitoB.mypos.domain.valueobjects.Currency
import com.copitoB.mypos.domain.valueobjects.Money
import com.copitoB.mypos.domain.valueobjects.PaymentMethod
import java.math.BigDecimal
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ReverseTransactionFacadeTest {

  private val driver = TestDb.createDriver()
  private val db = TpvDatabase(driver)
  private val saleRepository = SqliteSaleRepository(driver)
  private val productRepository = SqliteProductRepository(driver)
  private val expenseRepository = SqliteExpenseRepository(driver)
  private val accumulatorRepository = SqliteAccumulatorRepository(driver)
  private val categoryRepository = SqliteCategoryRepository(driver)
  private val updateAccumulator = UpdateAccumulator(accumulatorRepository)

  private val revertTransaction = RevertTransaction(saleRepository, productRepository, updateAccumulator)
  private val reverseTransaction = ReverseTransaction(saleRepository, revertTransaction)

  @AfterTest
  fun tearDown() {
    driver.close()
  }

  private fun money(value: String) = Money(BigDecimal(value), Currency.Bs)

  private suspend fun bootstrapAccumulator() {
    accumulatorRepository.updateAccumulator(Money.zero(), Money.zero(), Money.zero())
  }

  private suspend fun seedProduct(stock: String = "10.000"): Long {
    val categoryId = categoryRepository.create("Cat").id!!
    val product = productRepository.create(
      ProductInput(
        categoryId = categoryId,
        sku = null,
        name = "Prod",
        sellingPrice = "10.00",
        purchasePrice = "5.00",
        currentStock = stock,
        minStock = "2.000",
        imageUri = null
      )
    )
    return product.id!!
  }

  @Test
  fun `revert sale clears accumulator and soft deletes`() = kotlinx.coroutines.runBlocking {
    bootstrapAccumulator()
    val productId = seedProduct("10.000")

    val sale = saleRepository.create("SALE", money("30.00"), PaymentMethod.CASH, "sale")
    saleRepository.addDetail(sale.id!!, productId, 3.0, 10.0)
    accumulatorRepository.updateAccumulator(money("30.00"), Money.zero(), Money.zero())

    reverseTransaction.execute(sale.id!!)

    assertNull(saleRepository.findById(sale.id!!))
    val summary = accumulatorRepository.getCashSummary()
    assertEquals(money("0.00"), summary.totalSales)
    assertEquals(money("0.00"), summary.netBalance)
  }

  @Test
  fun `revert purchase with insufficient stock throws and leaves transaction intact`() = kotlinx.coroutines.runBlocking {
    bootstrapAccumulator()
    val productId = seedProduct("2.000")

    val purchase = saleRepository.create("PURCHASE", money("50.00"), PaymentMethod.TRANSFER, "purchase")
    saleRepository.addDetail(purchase.id!!, productId, 5.0, 10.0)
    accumulatorRepository.updateAccumulator(Money.zero(), money("50.00"), Money.zero())

    assertFailsWith<StockError.Insufficient> {
      reverseTransaction.execute(purchase.id!!)
    }

    assertNotNull(saleRepository.findById(purchase.id!!))
    assertEquals(money("50.00"), accumulatorRepository.getCashSummary().totalPurchases)
  }

  @Test
  fun `revert expense clears total expenses without stock change`() = kotlinx.coroutines.runBlocking {
    bootstrapAccumulator()
    val categoryId = expenseRepository.createCategory("Cat").id!!
    val expense = expenseRepository.create(
      ExpenseInput(categoryId = categoryId, amount = money("20.00"), description = "e", paymentMethod = PaymentMethod.QR)
    )
    accumulatorRepository.updateAccumulator(Money.zero(), Money.zero(), money("20.00"))

    reverseTransaction.execute(expense.id!!)

    assertNull(expenseRepository.findById(expense.id!!))
    assertEquals(money("0.00"), accumulatorRepository.getCashSummary().totalExpenses)
  }

  @Test
  fun `double revert throws already_reversed`() = kotlinx.coroutines.runBlocking {
    bootstrapAccumulator()
    val productId = seedProduct("10.000")
    val sale = saleRepository.create("SALE", money("30.00"), PaymentMethod.CASH, "sale")
    saleRepository.addDetail(sale.id!!, productId, 3.0, 10.0)
    accumulatorRepository.updateAccumulator(money("30.00"), Money.zero(), Money.zero())

    reverseTransaction.execute(sale.id!!)

    val error = assertFailsWith<IllegalStateException> {
      reverseTransaction.execute(sale.id!!)
    }
    assertEquals("already_reversed", error.message)
  }
}