package com.copitoB.mypos.application.transactions

import com.copitoB.mypos.domain.repository.ProductRepository
import com.copitoB.mypos.domain.repository.SaleRepository
import com.copitoB.mypos.domain.rules.StockRules
import com.copitoB.mypos.domain.value-objects.Stock
import java.math.BigDecimal

class RevertTransaction(
  private val saleRepository: SaleRepository,
  private val productRepository: ProductRepository
) {
  data class Result(
    val transactionId: Long,
    val type: String,
    val stockRestored: Boolean
  )

  suspend fun execute(transactionId: Long): Result {
    val transaction = saleRepository.findById(transactionId)
      ?: throw IllegalArgumentException("Transaction not found: $transactionId")

    when (transaction.type) {
      "SALE" -> {
        // Restore stock for each detail
        val details = saleRepository.listDetails(transactionId)
        for (detail in details) {
          val product = productRepository.findById(detail.productId)
            ?: throw IllegalArgumentException("Product not found: ${detail.productId}")
          val newStock = Stock(BigDecimal(product.currentStock.value.toString())).plus(
            Stock(BigDecimal(detail.quantity.toString()))
          )
          productRepository.adjustStock(detail.productId, newStock, "Revert sale: $transactionId")
        }
      }
      "PURCHASE" -> {
        // Decrement stock, but check it won't go negative first
        val details = saleRepository.listDetails(transactionId)
        for (detail in details) {
          val product = productRepository.findById(detail.productId)
            ?: throw IllegalArgumentException("Product not found: ${detail.productId}")
          StockRules.validateStockAvailable(
            product.currentStock,
            Stock(BigDecimal(detail.quantity.toString()))
          )
          val newStock = Stock(BigDecimal(product.currentStock.value.toString())).minus(
            Stock(BigDecimal(detail.quantity.toString()))
          )
          productRepository.adjustStock(detail.productId, newStock, "Revert purchase: $transactionId")
        }
      }
      "EXPENSE" -> {
        // No stock changes for expenses
      }
      else -> throw IllegalArgumentException("Unknown transaction type: ${transaction.type}")
    }

    saleRepository.softDelete(transactionId)

    return Result(
      transactionId = transactionId,
      type = transaction.type,
      stockRestored = transaction.type != "EXPENSE"
    )
  }
}
