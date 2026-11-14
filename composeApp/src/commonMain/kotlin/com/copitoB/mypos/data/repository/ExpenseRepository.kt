package com.copitoB.mypos.data.repository

import com.copitoB.mypos.domain.entities.Expense
import com.copitoB.mypos.domain.entities.ExpenseCategory
import com.copitoB.mypos.domain.entities.ExpenseInput
import com.copitoB.mypos.domain.value-objects.Money
import app.cash.sqldelight.CoroutinesExtensions.awaitFirstOrNull
import app.cash.sqldelight.CoroutinesExtensions.asFlow
import app.cash.sqldelight.db
import com.copitoB.mypos.data.repository.TpvDatabase.Companion.Driver
import kotlinx.coroutines.flow.flow

interface ExpenseRepository {
  suspend fun create(input: ExpenseInput): Expense
  suspend fun findById(id: Long): Expense?
  suspend fun list(limit: Int, offset: Int): List<Expense>
  suspend fun listByMonth(month: String): List<Expense>
  suspend fun softDelete(id: Long)
  suspend fun totalExpenses(): Money
  suspend fun listCategories(): List<ExpenseCategory>
  suspend fun findCategoryById(id: Long): ExpenseCategory?
  suspend fun createCategory(name: String): ExpenseCategory
  suspend fun softDeleteCategory(id: Long)
  fun watchExpenses(): Flow<List<Expense>>
}

class SqliteExpenseRepository(
  private val driver: Driver
) : ExpenseRepository {
  private val queries = com.copitoB.mypos.data.repository.TpvDatabase(driver)

  override suspend fun create(input: ExpenseInput): Expense {
    return queries.db.withTransaction {
      queries.SalesTransactionsQueries.insert(
        type = "EXPENSE",
        totalAmount = input.amount.toDouble(),
        paymentMethod = input.paymentMethod,
        detail = input.description,
        registeredAt = java.time.Instant.now().toString()
      )
      val id = queries.db.lastInsertRowId
      findById(id)!!
    }
  }

  override suspend fun findById(id: Long): Expense? {
    return queries.SalesTransactionsQueries.findById(id = id).awaitFirstOrNull()?.let { mapExpense(it) }
  }

  override suspend fun list(limit: Int, offset: Int): List<Expense> {
    return queries.SalesTransactionsQueries.listExpenses(limit = limit, offset = offset).asList().map { mapExpense(it) }
  }

  override suspend fun listByMonth(month: String): List<Expense> {
    return queries.SalesTransactionsQueries.listExpensesByMonth(month = month).asList().map { mapExpense(it) }
  }

  override suspend fun softDelete(id: Long) {
    queries.db.withTransaction {
      queries.SalesTransactionsQueries.softDelete(id = id, deletedAt = java.time.Instant.now().toString())
    }
  }

  override suspend fun totalExpenses(): Money {
    val total = queries.SalesTransactionsQueries.sumExpenses()
    return Money(java.math.BigDecimal(total.toString()), com.copitoB.mypos.domain.value-objects.Currency.Bs)
  }

  override suspend fun listCategories(): List<ExpenseCategory> {
    return queries.ExpenseCategoryQueries.findAll().asList().map { mapCategory(it) }
  }

  override suspend fun findCategoryById(id: Long): ExpenseCategory? {
    return queries.ExpenseCategoryQueries.findById(id = id).awaitFirstOrNull()?.let { mapCategory(it) }
  }

  override suspend fun createCategory(name: String): ExpenseCategory {
    return queries.db.withTransaction {
      queries.ExpenseCategoryQueries.insert(name = name, registeredAt = java.time.Instant.now().toString())
      val id = queries.db.lastInsertRowId
      findCategoryById(id)!!
    }
  }

  override suspend fun softDeleteCategory(id: Long) {
    queries.db.withTransaction {
      queries.ExpenseCategoryQueries.softDelete(id = id, deletedAt = java.time.Instant.now().toString())
    }
  }

  override fun watchExpenses(): Flow<List<Expense>> = flow {
    emit(queries.SalesTransactionsQueries.listExpenses(limit = 100, offset = 0).asList().map { mapExpense(it) })
  }

  private fun mapExpense(row: com.copitoB.mypos.data.repository.TpvDatabase.SaleTransaction): Expense {
    return Expense(
      id = row.id,
      categoryId = 1L, // Default category - expenses don't have category_id in shared table
      amount = Money(java.math.BigDecimal(row.totalAmount.toString()), com.copitoB.mypos.domain.value-objects.Currency.Bs),
      description = row.detail ?: "",
      paymentMethod = row.paymentMethod,
      registeredAt = row.registeredAt,
      deletedAt = row.deletedAt
    )
  }

  private fun mapCategory(row: com.copitoB.mypos.data.repository.TpvDatabase.ExpenseCategory): ExpenseCategory {
    return ExpenseCategory(
      id = row.id,
      name = row.name,
      registeredAt = row.registeredAt,
      deletedAt = row.deletedAt
    )
  }
}
