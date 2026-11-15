package com.copitoB.mypos.domain.entities

import com.copitoB.mypos.domain.valueobjects.Money
import com.copitoB.mypos.domain.valueobjects.PaymentMethod

data class SaleTransaction(
  val id: Long? = null,
  val type: String,
  val totalAmount: Money,
  val paymentMethod: PaymentMethod,
  val detail: String? = null,
  val registeredAt: String? = null,
  val deletedAt: String? = null
)
