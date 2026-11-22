package com.copitoB.mypos.domain.repository

import com.copitoB.mypos.domain.entities.Product
import com.copitoB.mypos.domain.entities.ProductInput
import com.copitoB.mypos.domain.entities.ProductSort
import com.copitoB.mypos.domain.valueobjects.Stock
import kotlinx.coroutines.flow.Flow

interface ProductRepository {
  suspend fun create(input: ProductInput): Product
  suspend fun findById(id: Long): Product?
  suspend fun findBySku(sku: String): Product?
  suspend fun list(
    search: String? = null,
    categoryId: Long? = null,
    limit: Int,
    offset: Int,
    sort: ProductSort = ProductSort.NOMBRE_ASC,
    lowStockOnly: Boolean = false
  ): List<Product>
  suspend fun listLowStock(): List<Product>
  suspend fun update(id: Long, input: ProductInput): Product
  suspend fun softDelete(id: Long)
  suspend fun adjustStock(productId: Long, newStock: Stock, reason: String)
  fun watchAll(): Flow<List<Product>>
}
