package com.copitoB.mypos.data.repository

import app.cash.sqldelight.db.SqlDriver
import com.copitoB.mypos.domain.entities.SaleTransaction
import com.copitoB.mypos.domain.entities.TransactionDetail
import com.copitoB.mypos.domain.repository.SaleRepository
import com.copitoB.mypos.domain.valueobjects.Currency
import com.copitoB.mypos.domain.valueobjects.Money
import com.copitoB.mypos.domain.valueobjects.PaymentMethod
import com.copitoB.mypos.data.repository.TpvDatabase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class SqliteSaleRepository(
  private val driver: SqlDriver
) : SaleRepository {
  private val queries = TpvDatabase(driver)

  override suspend fun create(type: String, totalAmount: Money, paymentMethod: PaymentMethod, detail: String?): SaleTransaction {
    var id: Long = 0
    queries.salesQueries.transaction(true) {
      queries.salesQueries.salesInsert(
        type = type,
        totalAmount = totalAmount.amount.toDouble(),
        paymentMethod = paymentMethod.code,
        detail = detail,
        registeredAt = java.time.Instant.now().toString()
      )
      id = queries.salesQueries.lastInsertId().executeAsOne()
    }
    return findById(id)!!
  }

  override suspend fun findById(id: Long): SaleTransaction? {
    return queries.salesQueries.salesFindById(id = id).executeAsList()
      .firstOrNull()?.let { mapTransaction(it) }
  }

  override suspend fun list(type: String?, limit: Int, offset: Int): List<SaleTransaction> {
    return queries.salesQueries.salesFindByType(type = type ?: "", limit = limit.toLong(), offset = offset.toLong()).executeAsList()
      .map { mapTransaction(it) }
  }

  override suspend fun softDelete(id: Long) {
    queries.salesQueries.transaction(true) {
      queries.salesQueries.salesSoftDelete(id = id, deletedAt = java.time.Instant.now().toString())
    }
  }

  override suspend fun addDetail(transactionId: Long, productId: Long, quantity: Double, unitPrice: Double) {
    queries.salesQueries.transaction(true) {
      queries.salesQueries.detailsInsert(
        transactionId = transactionId,
        productId = productId,
        quantity = quantity,
        unitPrice = unitPrice
      )
    }
  }

  override suspend fun listDetails(transactionId: Long): List<TransactionDetail> {
    return queries.salesQueries.detailsFindByTransaction(transactionId = transactionId).executeAsList()
      .map { mapDetail(it) }
  }

  override fun watchAll(): Flow<List<SaleTransaction>> = flow {
    emit(queries.salesQueries.salesFindAll().executeAsList().map { mapTransaction(it) })
  }

  private fun mapTransaction(row: com.copitoB.mypos.data.repository.Sales_transactions): SaleTransaction {
    return SaleTransaction(
      id = row.id,
      type = row.type,
      totalAmount = Money(java.math.BigDecimal(row.total_amount.toString()), Currency.Bs),
      paymentMethod = PaymentMethod.valueOf(row.payment_method),
      detail = row.detail,
      registeredAt = row.registered_at,
      deletedAt = row.deleted_at
    )
  }

  private fun mapDetail(row: com.copitoB.mypos.data.repository.Transaction_details): TransactionDetail {
    return TransactionDetail(
      id = row.id,
      transactionId = row.transaction_id,
      productId = row.product_id,
      quantity = row.quantity,
      unitPrice = row.unit_price
    )
  }
}