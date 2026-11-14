package com.copitoB.mypos.domain.entities

import com.copitoB.mypos.domain.value-objects.Stock

data class ProductInput(
  val categoryId: Long,
  val sku: String?,
  val name: String,
  val sellingPrice: String,
  val purchasePrice: String,
  val currentStock: String,
  val minStock: String,
  val imageUri: String?
)

data class StockAdjustment(
  val productId: Long,
  val delta: Stock,
  val reason: String
)

data class LowStockAlert(
  val product: Product,
  val currentStock: Stock,
  val minStock: Stock
)
