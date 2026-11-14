package com.copitoB.mypos.domain.entities

import com.copitoB.mypos.domain.value-objects.Money
import com.copitoB.mypos.domain.value-objects.PaymentMethod

data class SaleTransaction(
  val id: Long? = null,
  val type: String,
  val totalAmount: Money,
  val paymentMethod: PaymentMethod,
  val detail: String? = null,
  val registeredAt: String? = null,
  val deletedAt: String? = null
)

data class TransactionDetail(
  val id: Long? = null,
  val transactionId: Long,
  val productId: Long,
  val quantity: Double,
  val unitPrice: Double
)
