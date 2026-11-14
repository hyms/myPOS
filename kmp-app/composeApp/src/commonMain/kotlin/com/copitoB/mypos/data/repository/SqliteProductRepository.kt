package com.copitoB.mypos.data.repository

import com.copitoB.mypos.domain.entities.Category
import com.copitoB.mypos.domain.entities.Product
import com.copitoB.mypos.domain.entities.ProductInput
import com.copitoB.mypos.domain.value-objects.Money
import com.copitoB.mypos.domain.value-objects.SKU
import com.copitoB.mypos.domain.value-objects.Stock
import app.cash.sqldelight.CoroutinesExtensions.awaitFirstOrNull
import app.cash.sqldelight.CoroutinesExtensions.asFlow
import app.cash.sqldelight.db
import com.copitoB.mypos.data.repository.TpvDatabase.Companion.Driver
import kotlinx.coroutines.flow.flow

class SqliteProductRepository(
  private val driver: Driver
) : ProductRepository {
  private val queries = com.copitoB.mypos.data.repository.TpvDatabase(driver)

  override suspend fun create(input: ProductInput): Product {
    return queries.db.withTransaction {
      queries.ProductQueries.insert(
        categoryId = input.categoryId,
        sku = input.sku,
        name = input.name,
        sellingPrice = input.sellingPrice.toDouble(),
        purchasePrice = input.purchasePrice.toDouble(),
        currentStock = input.currentStock.toDouble(),
        minStock = input.minStock.toDouble(),
        imageUri = input.imageUri,
        registeredAt = java.time.Instant.now().toString(),
        updatedAt = java.time.Instant.now().toString()
      )
      val id = queries.db.lastInsertRowId
      findById(id)!!
    }
  }

  override suspend fun findById(id: Long): Product? {
    return queries.ProductQueries.findById(id = id).awaitFirstOrNull()?.let { mapProduct(it) }
  }

  override suspend fun findBySku(sku: String): Product? {
    return queries.ProductQueries.findBySku(sku = sku).awaitFirstOrNull()?.let { mapProduct(it) }
  }

  override suspend fun list(search: String?, categoryId: Long?, limit: Int, offset: Int): List<Product> {
    return queries.ProductQueries.searchByName(search = search ?: "", limit = limit, offset = offset).asList().map { mapProduct(it) }
  }

  override suspend fun listLowStock(): List<Product> {
    return queries.ProductQueries.listLowStock().asList().map { mapProduct(it) }
  }

  override suspend fun update(id: Long, input: ProductInput): Product {
    return queries.db.withTransaction {
      queries.ProductQueries.update(
        id = id,
        categoryId = input.categoryId,
        sku = input.sku,
        name = input.name,
        sellingPrice = input.sellingPrice.toDouble(),
        purchasePrice = input.purchasePrice.toDouble(),
        currentStock = input.currentStock.toDouble(),
        minStock = input.minStock.toDouble(),
        imageUri = input.imageUri,
        updatedAt = java.time.Instant.now().toString()
      )
      findById(id)!!
    }
  }

  override suspend fun softDelete(id: Long) {
    queries.db.withTransaction {
      queries.ProductQueries.softDelete(id = id, deletedAt = java.time.Instant.now().toString())
    }
  }

  override suspend fun adjustStock(productId: Long, delta: Stock, reason: String) {
    queries.db.withTransaction {
      val current = queries.ProductQueries.findById(id = productId).awaitFirstOrNull()!!
      val newStock = Stock(BigDecimal(current.currentStock.toString())).plus(delta)
      queries.ProductQueries.update(
        id = productId,
        categoryId = current.categoryId,
        sku = current.sku?.value,
        name = current.name,
        sellingPrice = current.sellingPrice,
        purchasePrice = current.purchasePrice,
        currentStock = newStock.value.toDouble(),
        minStock = current.minStock,
        imageUri = current.imageUri,
        updatedAt = java.time.Instant.now().toString()
      )
    }
  }

  override fun watchAll(): Flow<List<Product>> = flow {
    emit(queries.ProductQueries.findAll().asList().map { mapProduct(it) })
  }

  private fun mapProduct(row: com.copitoB.mypos.data.repository.TpvDatabase.Product): Product {
    return Product(
      id = row.id,
      categoryId = row.categoryId,
      sku = row.sku?.let { SKU.create(it) },
      name = row.name,
      sellingPrice = Money(java.math.BigDecimal(row.sellingPrice.toString()), com.copitoB.mypos.domain.value-objects.Currency.Bs),
      purchasePrice = Money(java.math.BigDecimal(row.purchasePrice.toString()), com.copitoB.mypos.domain.value-objects.Currency.Bs),
      currentStock = Stock(java.math.BigDecimal(row.currentStock.toString())),
      minStock = Stock(java.math.BigDecimal(row.minStock.toString())),
      imageUri = row.imageUri,
      registeredAt = row.registeredAt,
      updatedAt = row.updatedAt,
      deletedAt = row.deletedAt
    )
  }
}

import java.math.BigDecimal

class SqliteCategoryRepository(
  private val driver: com.copitoB.mypos.data.repository.TpvDatabase.Companion.Driver
) : CategoryRepository {
  private val queries = com.copitoB.mypos.data.repository.TpvDatabase(driver)

  override suspend fun create(name: String): Category {
    return queries.db.withTransaction {
      queries.CategoryQueries.insert(name = name, registeredAt = java.time.Instant.now().toString())
      val id = queries.db.lastInsertRowId
      findById(id)!!
    }
  }

  override suspend fun findById(id: Long): Category? {
    return queries.CategoryQueries.findById(id = id).awaitFirstOrNull()?.let { mapCategory(it) }
  }

  override suspend fun list(): List<Category> {
    return queries.CategoryQueries.findAll().asList().map { mapCategory(it) }
  }

  override suspend fun update(id: Long, name: String): Category {
    return queries.db.withTransaction {
      queries.CategoryQueries.update(id = id, name = name, updatedAt = java.time.Instant.now().toString())
      findById(id)!!
    }
  }

  override suspend fun softDelete(id: Long) {
    queries.db.withTransaction {
      queries.CategoryQueries.softDelete(id = id, deletedAt = java.time.Instant.now().toString())
    }
  }

  override fun watchAll(): kotlinx.coroutines.flow.Flow<List<Category>> = kotlinx.coroutines.flow.flow {
    emit(queries.CategoryQueries.findAll().asList().map { mapCategory(it) })
  }

  private fun mapCategory(row: com.copitoB.mypos.data.repository.TpvDatabase.Category): Category {
    return Category(
      id = row.id,
      name = row.name,
      registeredAt = row.registeredAt,
      deletedAt = row.deletedAt
    )
  }
}
