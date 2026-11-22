package com.copitoB.mypos.domain.repository

import com.copitoB.mypos.domain.entities.SaleTransaction
import com.copitoB.mypos.domain.entities.TransactionDetail
import com.copitoB.mypos.domain.valueobjects.Money
import com.copitoB.mypos.domain.valueobjects.PaymentMethod
import kotlinx.coroutines.flow.Flow

interface SaleRepository {
  suspend fun create(type: String, totalAmount: Money, paymentMethod: PaymentMethod, detail: String?): SaleTransaction
  suspend fun findById(id: Long): SaleTransaction?
  suspend fun findByIdIncludingDeleted(id: Long): SaleTransaction?
  suspend fun list(type: String?, limit: Int, offset: Int): List<SaleTransaction>
  suspend fun listMovements(type: String?, search: String?, from: String?, to: String?, limit: Int, offset: Int): List<SaleTransaction>
  suspend fun countMovements(type: String?, search: String?, from: String?, to: String?): Long
  suspend fun softDelete(id: Long)
  suspend fun addDetail(transactionId: Long, productId: Long, quantity: Double, unitPrice: Double)
  suspend fun listDetails(transactionId: Long): List<TransactionDetail>
  fun watchAll(): Flow<List<SaleTransaction>>
}