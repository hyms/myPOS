package com.copitoB.mypos.domain.entities

import com.copitoB.mypos.domain.value-objects.Money

data class Expense(
  val id: Long? = null,
  val categoryId: Long,
  val amount: Money,
  val description: String,
  val paymentMethod: String,
  val registeredAt: String? = null,
  val deletedAt: String? = null
)

data class ExpenseCategory(
  val id: Long? = null,
  val name: String,
  val registeredAt: String? = null,
  val deletedAt: String? = null
)

data class ExpenseInput(
  val categoryId: Long,
  val amount: String,
  val description: String,
  val paymentMethod: String
)
