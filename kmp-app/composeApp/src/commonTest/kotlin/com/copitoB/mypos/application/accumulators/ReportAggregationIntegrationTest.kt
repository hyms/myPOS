package com.copitoB.mypos.application.accumulators

import com.copitoB.mypos.application.backup.TestDb
import com.copitoB.mypos.data.repository.SqliteAccumulatorRepository
import com.copitoB.mypos.data.repository.SqliteExpenseRepository
import com.copitoB.mypos.data.repository.SqliteSaleRepository
import com.copitoB.mypos.data.repository.TpvDatabase
import com.copitoB.mypos.domain.valueobjects.Currency
import com.copitoB.mypos.domain.valueobjects.Money
import com.copitoB.mypos.domain.valueobjects.PaymentMethod
import com.copitoB.mypos.domain.valueobjects.TransactionType
import java.math.BigDecimal
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals

class ReportAggregationIntegrationTest {

  private val driver = TestDb.createDriver()
  private val db = TpvDatabase(driver)
  private val saleRepository = SqliteSaleRepository(driver)
  private val expenseRepository = SqliteExpenseRepository(driver)
  private val accumulatorRepository = SqliteAccumulatorRepository(driver)

  @AfterTest
  fun tearDown() {
    driver.close()
  }

  private fun money(value: String) = Money(BigDecimal(value), Currency.Bs)

  private suspend fun seed(): Long {
    val cashSale = saleRepository.create("SALE", money("100.00"), PaymentMethod.CASH, "sale-cash")
    saleRepository.create("SALE", money("50.00"), PaymentMethod.CARD, "sale-card")
    saleRepository.create("PURCHASE", money("40.00"), PaymentMethod.TRANSFER, "purchase")
    expenseRepository.create(
      com.copitoB.mypos.domain.entities.ExpenseInput(
        categoryId = expenseRepository.createCategory("Cat").id!!,
        amount = money("10.00"),
        description = "expense",
        paymentMethod = PaymentMethod.QR
      )
    )
    return cashSale.id!!
  }

  @Test
  fun `payment method breakdown groups sales only`() = kotlinx.coroutines.runBlocking {
    seed()
    val breakdown = accumulatorRepository.getSalesByPaymentMethod(null, null)
    assertEquals(money("100.00"), breakdown[PaymentMethod.CASH])
    assertEquals(money("50.00"), breakdown[PaymentMethod.CARD])
    assertEquals(money("0.00"), breakdown[PaymentMethod.TRANSFER])
    assertEquals(money("0.00"), breakdown[PaymentMethod.QR])
  }

  @Test
  fun `range totals compute net across all types`() = kotlinx.coroutines.runBlocking {
    seed()
    val totals = accumulatorRepository.getTotalsInRange(null, null)
    assertEquals(money("150.00"), totals.sales)
    assertEquals(money("40.00"), totals.purchases)
    assertEquals(money("10.00"), totals.expenses)
    assertEquals(money("100.00"), totals.net)
  }

  @Test
  fun `monthly and annual breakdown rows reflect totals`() = kotlinx.coroutines.runBlocking {
    seed()
    val monthly = accumulatorRepository.getMonthlyBreakdown(null, null)
    assertEquals(1, monthly.size)
    val row = monthly.first()
    assertEquals(money("150.00"), row.sales)
    assertEquals(money("40.00"), row.purchases)
    assertEquals(money("10.00"), row.expenses)
    assertEquals(money("100.00"), row.net)

    val annual = accumulatorRepository.getAnnualBreakdown(null, null)
    assertEquals(1, annual.size)
    assertEquals(money("150.00"), annual.first().sales)
  }

  @Test
  fun `soft deleted transactions excluded from aggregations`() = kotlinx.coroutines.runBlocking {
    val cashSaleId = seed()
    saleRepository.softDelete(cashSaleId)

    val breakdown = accumulatorRepository.getSalesByPaymentMethod(null, null)
    assertEquals(money("0.00"), breakdown[PaymentMethod.CASH])
    assertEquals(money("50.00"), breakdown[PaymentMethod.CARD])

    val totals = accumulatorRepository.getTotalsInRange(null, null)
    assertEquals(money("50.00"), totals.sales)
  }
}