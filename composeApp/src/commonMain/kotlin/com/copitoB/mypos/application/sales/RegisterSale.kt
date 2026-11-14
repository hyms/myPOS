package com.copitoB.mypos.application.sales

import com.copitoB.mypos.domain.entities.ICartItem
import com.copitoB.mypos.domain.entities.SaleTransaction
import com.copitoB.mypos.domain.repository.ProductRepository
import com.copitoB.mypos.domain.repository.SaleRepository
import com.copitoB.mypos.domain.rules.CartRules
import com.copitoB.mypos.domain.rules.StockRules
import com.copitoB.mypos.domain.value-objects.Currency
import com.copitoB.mypos.domain.value-objects.Money
import com.copitoB.mypos.domain.value-objects.PaymentMethod
import com.copitoB.mypos.domain.value-objects.Stock
import java.math.BigDecimal

class RegisterSale(
  private val saleRepository: SaleRepository,
  private val productRepository: ProductRepository
) {
  data class Result(
    val transaction: SaleTransaction,
    val totals: CartRules.CartTotals,
    val change: Money?
  )

  suspend fun execute(
    items: List<ICartItem>,
    paymentMethod: PaymentMethod,
    received: Money?,
    detail: String? = null
  ): Result {
    require(items.isNotEmpty()) { "Cart cannot be empty" }

    val totals = CartRules.calcTotals(items)

    // Validate stock for each item
    for (item in items) {
      val product = productRepository.findById(item.productId)
        ?: throw IllegalArgumentException("Product not found: ${item.productId}")
      StockRules.validateStockAvailable(
        product.currentStock,
        Stock(java.math.BigDecimal(item.quantity.toString()))
      )
    }

    val totalMoney = Money(BigDecimal(totals.total), Currency.Bs)

    return saleRepository.create(
      type = "SALE",
      totalAmount = totalMoney,
      paymentMethod = paymentMethod,
      detail = detail
    ).let { transaction ->
      // Add transaction details
      for (item in items) {
        saleRepository.addDetail(
          transactionId = transaction.id!!,
          productId = item.productId,
          quantity = item.quantity,
          unitPrice = item.unitPrice
        )
        // Decrement stock
        val product = productRepository.findById(item.productId)!!
        val newStock = Stock(BigDecimal(product.currentStock.value.toString())).minus(
          Stock(BigDecimal(item.quantity.toString()))
        )
        productRepository.adjustStock(item.productId, newStock, "Sale: $detail")
      }

      val change = received?.let { r ->
        CartRules.calcChange(total = totalMoney, received = r)
      }

      Result(transaction = transaction, totals = totals, change = change)
    }
  }
}

class RegisterPurchase(
  private val saleRepository: SaleRepository,
  private val productRepository: ProductRepository
) {
  data class Result(val transaction: SaleTransaction, val totals: CartRules.CartTotals)

  suspend fun execute(
    items: List<ICartItem>,
    paymentMethod: PaymentMethod,
    detail: String? = null
  ): Result {
    require(items.isNotEmpty()) { "Cart cannot be empty" }

    val totals = CartRules.calcTotals(items)
    val totalMoney = Money(BigDecimal(totals.total), Currency.Bs)

    return saleRepository.create(
      type = "PURCHASE",
      totalAmount = totalMoney,
      paymentMethod = paymentMethod,
      detail = detail
    ).let { transaction ->
      for (item in items) {
        saleRepository.addDetail(
          transactionId = transaction.id!!,
          productId = item.productId,
          quantity = item.quantity,
          unitPrice = item.unitPrice
        )
        // Increment stock for purchases
        val product = productRepository.findById(item.productId)!!
        val newStock = Stock(BigDecimal(product.currentStock.value.toString())).plus(
          Stock(BigDecimal(item.quantity.toString()))
        )
        productRepository.adjustStock(item.productId, newStock, "Purchase: $detail")
      }
      Result(transaction = transaction, totals = totals)
    }
  }
}
