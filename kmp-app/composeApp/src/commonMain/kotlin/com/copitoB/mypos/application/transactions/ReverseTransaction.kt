package com.copitoB.mypos.application.transactions

import com.copitoB.mypos.domain.repository.SaleRepository
import com.copitoB.mypos.domain.valueobjects.TransactionType

class ReverseTransaction(
  private val saleRepository: SaleRepository,
  private val revertTransaction: RevertTransaction
) {
  data class Result(
    val transactionId: Long,
    val type: TransactionType
  )

  suspend fun execute(transactionId: Long): Result {
    val transaction = saleRepository.findByIdIncludingDeleted(transactionId)
      ?: throw IllegalArgumentException("Transaction not found: $transactionId")

    if (transaction.deletedAt != null) throw IllegalStateException("already_reversed")

    val type = runCatching { TransactionType.valueOf(transaction.type) }
      .getOrElse { throw IllegalArgumentException("Unknown transaction type: ${transaction.type}") }

    revertTransaction.execute(transactionId)

    return Result(transactionId = transactionId, type = type)
  }
}