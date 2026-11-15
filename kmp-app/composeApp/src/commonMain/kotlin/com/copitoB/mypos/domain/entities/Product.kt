package com.copitoB.mypos.domain.entities

import com.copitoB.mypos.domain.valueobjects.Money
import com.copitoB.mypos.domain.valueobjects.SKU
import com.copitoB.mypos.domain.valueobjects.Stock

data class Product(
  val id: Long? = null,
  val categoryId: Long,
  val sku: SKU?,
  val name: String,
  val sellingPrice: Money,
  val purchasePrice: Money,
  val currentStock: Stock,
  val minStock: Stock,
  val imageUri: String? = null,
  val registeredAt: String? = null,
  val updatedAt: String? = null,
  val deletedAt: String? = null
) {
  fun isBelowMinStock(): Boolean = currentStock.isBelow(minStock)
}

data class Category(
  val id: Long? = null,
  val name: String,
  val registeredAt: String? = null,
  val deletedAt: String? = null
)
