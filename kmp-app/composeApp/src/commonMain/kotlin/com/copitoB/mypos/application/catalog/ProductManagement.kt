package com.copitoB.mypos.application.catalog

import com.copitoB.mypos.domain.entities.Product
import com.copitoB.mypos.domain.entities.ProductInput
import com.copitoB.mypos.domain.repository.ProductRepository
import com.copitoB.mypos.domain.rules.StockRules
import com.copitoB.mypos.domain.value-objects.Currency
import com.copitoB.mypos.domain.value-objects.Money
import com.copitoB.mypos.domain.value-objects.SKU
import com.copitoB.mypos.domain.value-objects.Stock
import java.math.BigDecimal

class ProductManagement(
  private val productRepository: ProductRepository
) {
  suspend fun createProduct(input: ProductInput): Product {
    val sku = input.sku?.let { SKU.create(it) }

    if (sku != null) {
      val existing = productRepository.findBySku(sku.value)
      if (existing != null) {
        throw IllegalArgumentException("SKU already exists: ${sku.value}")
      }
    }

    return productRepository.create(input)
  }

  suspend fun getProduct(id: Long): Product? {
    return productRepository.findById(id)
  }

  suspend fun listProducts(search: String?, categoryId: Long?, limit: Int, offset: Int): List<Product> {
    return productRepository.list(search, categoryId, limit, offset)
  }

  suspend fun updateProduct(id: Long, input: ProductInput): Product {
    return productRepository.update(id, input)
  }

  suspend fun deleteProduct(id: Long) {
    productRepository.softDelete(id)
  }

  suspend fun adjustStock(productId: Long, newStock: Stock, reason: String) {
    val current = productRepository.findById(productId)
      ?: throw IllegalArgumentException("Product not found: $productId")
    StockRules.validateNonNegative(newStock)
    productRepository.adjustStock(productId, newStock, reason)
  }

  suspend fun getLowStockProducts(): List<Product> {
    return productRepository.listLowStock()
  }
}
