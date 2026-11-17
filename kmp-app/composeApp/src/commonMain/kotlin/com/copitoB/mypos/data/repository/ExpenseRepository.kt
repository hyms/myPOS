package com.copitoB.mypos.data.repository

import app.cash.sqldelight.db.SqlDriver
import com.copitoB.mypos.data.repository.TpvDatabase
import com.copitoB.mypos.domain.entities.Expense
import com.copitoB.mypos.domain.entities.ExpenseCategory
import com.copitoB.mypos.domain.entities.ExpenseInput
import com.copitoB.mypos.domain.repository.ExpenseRepository
import com.copitoB.mypos.domain.valueobjects.Currency
import com.copitoB.mypos.domain.valueobjects.Money
import com.copitoB.mypos.domain.valueobjects.PaymentMethod
import com.copitoB.mypos.domain.valueobjects.toMoneyRounded
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class SqliteExpenseRepository(
  private val driver: SqlDriver
) : ExpenseRepository {
  private val queries = TpvDatabase(driver)

  override suspend fun create(input: ExpenseInput): Expense {
    var id: Long = 0
    queries.expensesQueries.transaction(true) {
      queries.expensesQueries.expenseInsert(
        type = "EXPENSE",
        totalAmount = input.amount.amount.toDouble(),
        paymentMethod = input.paymentMethod.code,
        detail = input.description,
        registeredAt = java.time.Instant.now().toString()
      )
      id = queries.expensesQueries.lastInsertId().executeAsOne()
    }
    return findById(id)!!
  }

  override suspend fun findById(id: Long): Expense? {
    return queries.expensesQueries.expenseFindById(id = id).executeAsList()
      .firstOrNull()?.let { mapExpense(it) }
  }

  override suspend fun list(limit: Int, offset: Int): List<Expense> {
    return queries.expensesQueries.expenseList(limit = limit.toLong(), offset = offset.toLong()).executeAsList().map { mapExpense(it) }
  }

  override suspend fun listByMonth(month: String): List<Expense> {
    return queries.expensesQueries.expenseListByMonth(month = month).executeAsList().map { mapExpense(it) }
  }

  override suspend fun softDelete(id: Long) {
    queries.expensesQueries.transaction(true) {
      queries.expensesQueries.expenseSoftDelete(id = id, deletedAt = java.time.Instant.now().toString())
    }
  }

  override suspend fun totalExpenses(): Money {
    val result = queries.expensesQueries.expenseSum().executeAsOne()
    return (result.SUM ?: 0.0).toMoneyRounded(Currency.Bs)
  }

  override suspend fun listCategories(): List<ExpenseCategory> {
    return queries.expensesQueries.expenseCategoriesFindAll().executeAsList().map { mapCategory(it) }
  }

  override suspend fun findCategoryById(id: Long): ExpenseCategory? {
    return queries.expensesQueries.expenseCategoriesFindById(id = id).executeAsList()
      .firstOrNull()?.let { mapCategory(it) }
  }

  override suspend fun createCategory(name: String): ExpenseCategory {
    var id: Long = 0
    queries.expensesQueries.transaction(true) {
      queries.expensesQueries.expenseCategoriesInsert(
        name = name,
        registeredAt = java.time.Instant.now().toString()
      )
      id = queries.expensesQueries.lastInsertIdCategory().executeAsOne()
    }
    return findCategoryById(id)!!
  }

  override suspend fun softDeleteCategory(id: Long) {
    queries.expensesQueries.transaction(true) {
      queries.expensesQueries.expenseCategoriesSoftDelete(id = id, deletedAt = java.time.Instant.now().toString())
    }
  }

  override fun watchExpenses(): Flow<List<Expense>> = flow {
    emit(queries.expensesQueries.expenseList(limit = 100, offset = 0).executeAsList().map { mapExpense(it) })
  }

  private fun mapExpense(row: Sales_transactions): Expense {
    return Expense(
      id = row.id,
      categoryId = null,
      amount = Money(java.math.BigDecimal(row.total_amount.toString()), Currency.Bs),
      description = row.detail ?: "",
      paymentMethod = PaymentMethod.valueOf(row.payment_method),
      registeredAt = row.registered_at,
      deletedAt = row.deleted_at
    )
  }

  private fun mapCategory(row: Expense_categories): ExpenseCategory {
    return ExpenseCategory(
      id = row.id,
      name = row.name,
      registeredAt = row.registered_at,
      deletedAt = row.deleted_at
    )
  }
}