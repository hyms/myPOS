package com.copitoB.mypos.domain.repository

import com.copitoB.mypos.domain.entities.Expense
import com.copitoB.mypos.domain.entities.ExpenseCategory
import com.copitoB.mypos.domain.entities.ExpenseInput
import com.copitoB.mypos.domain.valueobjects.Money
import kotlinx.coroutines.flow.Flow

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
