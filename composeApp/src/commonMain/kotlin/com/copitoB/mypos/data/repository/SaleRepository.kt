package com.copitoB.mypos.data.repository

import com.copitoB.mypos.domain.entities.SaleTransaction
import com.copitoB.mypos.domain.entities.TransactionDetail
import com.copitoB.mypos.domain.value-objects.Money
import com.copitoB.mypos.domain.value-objects.PaymentMethod
import app.cash.sqldelight.CoroutinesExtensions.awaitFirstOrNull
import app.cash.sqldelight.CoroutinesExtensions.asFlow
import app.cash.sqldelight.db
import com.copitoB.mypos.data.repository.TpvDatabase.Companion.Driver
import kotlinx.coroutines.flow.flow

interface SaleRepository {
  suspend fun create(type: String, totalAmount: Money, paymentMethod: PaymentMethod, detail: String?): SaleTransaction
  suspend fun findById(id: Long): SaleTransaction?
  suspend fun list(type: String?, limit: Int, offset: Int): List<SaleTransaction>
  suspend fun softDelete(id: Long)
  suspend fun addDetail(transactionId: Long, productId: Long, quantity: Double, unitPrice: Double)
  suspend fun listDetails(transactionId: Long): List<TransactionDetail>
  fun watchAll(): Flow<List<SaleTransaction>>
}

class SqliteSaleRepository(
  private val driver: Driver
) : SaleRepository {
  private val queries = com.copitoB.mypos.data.repository.TpvDatabase(driver)

  override suspend fun create(type: String, totalAmount: Money, paymentMethod: PaymentMethod, detail: String?): SaleTransaction {
    return queries.db.withTransaction {
      queries.SalesTransactionsQueries.insert(
        type = type,
        totalAmount = totalAmount.amount.toDouble(),
        paymentMethod = paymentMethod.code,
        detail = detail,
        registeredAt = java.time.Instant.now().toString()
      )
      val id = queries.db.lastInsertRowId
      findById(id)!!
    }
  }

  override suspend fun findById(id: Long): SaleTransaction? {
    return queries.SalesTransactionsQueries.findById(id = id).awaitFirstOrNull()?.let { mapTransaction(it) }
  }

  override suspend fun list(type: String?, limit: Int, offset: Int): List<SaleTransaction> {
    return queries.SalesTransactionsQueries.listByType(type = type ?: "", limit = limit, offset = offset).asList().map { mapTransaction(it) }
  }

  override suspend fun softDelete(id: Long) {
    queries.db.withTransaction {
      queries.SalesTransactionsQueries.softDelete(id = id, deletedAt = java.time.Instant.now().toString())
    }
  }

  override suspend fun addDetail(transactionId: Long, productId: Long, quantity: Double, unitPrice: Double) {
    queries.TransactionDetailsQueries.insert(
      transactionId = transactionId,
      productId = productId,
      quantity = quantity,
      unitPrice = unitPrice
    )
  }

  override suspend fun listDetails(transactionId: Long): List<TransactionDetail> {
    return queries.TransactionDetailsQueries.findByTransaction(transactionId = transactionId).asList().map { mapDetail(it) }
  }

  override fun watchAll(): Flow<List<SaleTransaction>> = flow {
    emit(queries.SalesTransactionsQueries.findAll().asList().map { mapTransaction(it) })
  }

  private fun mapTransaction(row: com.copitoB.mypos.data.repository.TpvDatabase.SaleTransaction): SaleTransaction {
    return SaleTransaction(
      id = row.id,
      type = row.type,
      totalAmount = Money(java.math.BigDecimal(row.totalAmount.toString()), com.copitoB.mypos.domain.value-objects.Currency.Bs),
      paymentMethod = PaymentMethod.valueOf(row.paymentMethod),
      detail = row.detail,
      registeredAt = row.registeredAt,
      deletedAt = row.deletedAt
    )
  }

  private fun mapDetail(row: com.copitoB.mypos.data.repository.TpvDatabase.TransactionDetail): TransactionDetail {
    return TransactionDetail(
      id = row.id,
      transactionId = row.transactionId,
      productId = row.productId,
      quantity = row.quantity,
      unitPrice = row.unitPrice
    )
  }
}
