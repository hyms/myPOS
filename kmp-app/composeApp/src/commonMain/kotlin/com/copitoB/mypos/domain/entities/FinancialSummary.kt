package com.copitoB.mypos.domain.entities

import com.copitoB.mypos.domain.value-objects.Money

data class FinancialSummary(
  val totalSales: Money,
  val totalPurchases: Money,
  val totalExpenses: Money,
  val netBalance: Money,
  val updatedAt: String? = null
)

data class PeriodSummary(
  val period: String,
  val sales: Money,
  val purchases: Money,
  val expenses: Money,
  val net: Money
)

data class Transaction(
  val id: Long,
  val type: String,
  val totalAmount: Double,
  val paymentMethod: String,
  val detail: String?,
  val registeredAt: String?
)
