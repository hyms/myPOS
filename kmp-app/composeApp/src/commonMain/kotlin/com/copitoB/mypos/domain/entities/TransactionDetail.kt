package com.copitoB.mypos.domain.entities

data class TransactionDetail(
  val id: Long? = null,
  val transactionId: Long,
  val productId: Long,
  val quantity: Double,
  val unitPrice: Double
)
