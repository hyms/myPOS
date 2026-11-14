package com.copitoB.mypos.data.repository

import com.copitoB.mypos.domain.entities.Category
import com.copitoB.mypos.domain.entities.Product
import com.copitoB.mypos.domain.entities.ProductInput
import com.copitoB.mypos.domain.value-objects.Stock
import kotlinx.coroutines.flow.Flow

interface ProductRepository {
  suspend fun create(input: ProductInput): Product
  suspend fun findById(id: Long): Product?
  suspend fun findBySku(sku: String): Product?
  suspend fun list(search: String?, categoryId: Long?, limit: Int, offset: Int): List<Product>
  suspend fun listLowStock(): List<Product>
  suspend fun update(id: Long, input: ProductInput): Product
  suspend fun softDelete(id: Long)
  suspend fun adjustStock(productId: Long, delta: Stock, reason: String)
  fun watchAll(): Flow<List<Product>>
}

interface CategoryRepository {
  suspend fun create(name: String): Category
  suspend fun findById(id: Long): Category?
  suspend fun list(): List<Category>
  suspend fun update(id: Long, name: String): Category
  suspend fun softDelete(id: Long)
  fun watchAll(): Flow<List<Category>>
}
