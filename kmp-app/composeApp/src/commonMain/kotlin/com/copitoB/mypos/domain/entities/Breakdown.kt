package com.copitoB.mypos.domain.entities

import com.copitoB.mypos.domain.valueobjects.Money

data class MonthlyBreakdown(
  val month: String,
  val sales: Money,
  val purchases: Money,
  val expenses: Money,
  val net: Money
)

data class AnnualBreakdown(
  val year: String,
  val sales: Money,
  val purchases: Money,
  val expenses: Money,
  val net: Money
)

data class MovementFilter(
  val type: com.copitoB.mypos.domain.valueobjects.TransactionType? = null,
  val search: String? = null,
  val from: String? = null,
  val to: String? = null
)

enum class ReportRange {
  TODAY,
  MONTH,
  CUSTOM
}