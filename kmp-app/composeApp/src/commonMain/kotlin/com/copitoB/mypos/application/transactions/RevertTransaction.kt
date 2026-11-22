package com.copitoB.mypos.application.transactions

import com.copitoB.mypos.application.accumulators.UpdateAccumulator
import com.copitoB.mypos.domain.repository.ProductRepository
import com.copitoB.mypos.domain.repository.SaleRepository
import com.copitoB.mypos.domain.rules.StockRules
import com.copitoB.mypos.domain.valueobjects.Money
import com.copitoB.mypos.domain.valueobjects.Stock
import com.copitoB.mypos.domain.valueobjects.TransactionType
import java.math.BigDecimal

class RevertTransaction(
  private val saleRepository: SaleRepository,
  private val productRepository: ProductRepository,
  private val updateAccumulator: UpdateAccumulator
) {
  data class Result(
    val transactionId: Long,
    val type: String,
    val stockRestored: Boolean
  )

  suspend fun execute(transactionId: Long): Result {
    val transaction = saleRepository.findByIdIncludingDeleted(transactionId)
      ?: throw IllegalArgumentException("Transaction not found: $transactionId")

    if (transaction.deletedAt != null) throw IllegalStateException("already_reversed")

    val type = runCatching { TransactionType.valueOf(transaction.type) }
      .getOrElse { throw IllegalArgumentException("Unknown transaction type: ${transaction.type}") }

    val totalMoney = transaction.totalAmount
    val zero = Money.zero(totalMoney.currency)

    // Reversal must never make stock negative (RULE-FA-04): validate before any mutation
    if (type == TransactionType.PURCHASE) {
      val details = saleRepository.listDetails(transactionId)
      for (detail in details) {
        val product = productRepository.findById(detail.productId)
          ?: throw IllegalArgumentException("Product not found: ${detail.productId}")
        StockRules.validateStockAvailable(
          product.currentStock,
          Stock(BigDecimal(detail.quantity.toString()))
        )
      }
    }

    when (type) {
      TransactionType.SALE -> {
        val details = saleRepository.listDetails(transactionId)
        for (detail in details) {
          val product = productRepository.findById(detail.productId)
            ?: throw IllegalArgumentException("Product not found: ${detail.productId}")
          val newStock = Stock(BigDecimal(product.currentStock.value.toString())).plus(
            Stock(BigDecimal(detail.quantity.toString()))
          )
          productRepository.adjustStock(detail.productId, newStock, "Revert sale: $transactionId")
        }
        updateAccumulator.revert(UpdateAccumulator.Deltas(
          salesDelta = totalMoney, purchasesDelta = zero, expensesDelta = zero
        ))
      }
      TransactionType.PURCHASE -> {
        val details = saleRepository.listDetails(transactionId)
        for (detail in details) {
          val product = productRepository.findById(detail.productId)!!
          val newStock = Stock(BigDecimal(product.currentStock.value.toString())).minus(
            Stock(BigDecimal(detail.quantity.toString()))
          )
          productRepository.adjustStock(detail.productId, newStock, "Revert purchase: $transactionId")
        }
        updateAccumulator.revert(UpdateAccumulator.Deltas(
          salesDelta = zero, purchasesDelta = totalMoney, expensesDelta = zero
        ))
      }
      TransactionType.EXPENSE -> {
        updateAccumulator.revert(UpdateAccumulator.Deltas(
          salesDelta = zero, purchasesDelta = zero, expensesDelta = totalMoney
        ))
      }
    }

    saleRepository.softDelete(transactionId)

    return Result(
      transactionId = transactionId,
      type = transaction.type,
      stockRestored = type != TransactionType.EXPENSE
    )
  }
}