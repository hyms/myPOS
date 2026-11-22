package com.copitoB.mypos.data.repository

import app.cash.sqldelight.db.SqlDriver
import com.copitoB.mypos.domain.entities.Product
import com.copitoB.mypos.domain.entities.ProductSort
import com.copitoB.mypos.domain.repository.ProductRepository
import com.copitoB.mypos.domain.valueobjects.Currency
import com.copitoB.mypos.domain.valueobjects.Money
import com.copitoB.mypos.domain.valueobjects.SKU
import com.copitoB.mypos.domain.valueobjects.Stock
import com.copitoB.mypos.domain.valueobjects.toMoneyRounded
import com.copitoB.mypos.domain.valueobjects.toStockRounded
import java.math.BigDecimal
import com.copitoB.mypos.data.repository.Products
import com.copitoB.mypos.data.repository.TpvDatabase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class SqliteProductRepository(
  private val driver: SqlDriver
) : ProductRepository {
  private val queries = TpvDatabase(driver)

  override suspend fun create(input: com.copitoB.mypos.domain.entities.ProductInput): Product {
    var id: Long = 0
    queries.productsQueries.transaction(true) {
      queries.productsQueries.insert(
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
      id = queries.productsQueries.lastInsertIdCategory().executeAsOne()
    }
    return findById(id)!!
  }

  override suspend fun findById(id: Long): Product? {
    return queries.productsQueries.findById(id = id).executeAsList()
      .firstOrNull()?.let { mapProduct(it) }
  }

  override suspend fun findBySku(sku: String): Product? {
    return queries.productsQueries.findBySku(sku = sku).executeAsList()
      .firstOrNull()?.let { mapProduct(it) }
  }

  override suspend fun list(
    search: String?,
    categoryId: Long?,
    limit: Int,
    offset: Int,
    sort: ProductSort,
    lowStockOnly: Boolean
  ): List<Product> {
    val low = if (lowStockOnly) 1L else 0L
    return if (sort == ProductSort.POPULARIDAD_DESC) {
      queries.productsQueries.listFilteredPopular(
        search = search,
        categoryId = categoryId,
        lowStockOnly = low,
        limit = limit.toLong(),
        offset = offset.toLong()
      ).executeAsList()
    } else {
      queries.productsQueries.listFiltered(
        search = search,
        categoryId = categoryId,
        lowStockOnly = low,
        limit = limit.toLong(),
        offset = offset.toLong()
      ).executeAsList()
    }.map { mapProduct(it) }
  }

  override suspend fun listLowStock(): List<Product> {
    return queries.productsQueries.listLowStock().executeAsList().map { mapProduct(it) }
  }

  override suspend fun update(id: Long, input: com.copitoB.mypos.domain.entities.ProductInput): Product {
    queries.productsQueries.transaction(true) {
      queries.productsQueries.update(
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
    }
    return findById(id)!!
  }

  override suspend fun softDelete(id: Long) {
    queries.productsQueries.transaction(true) {
      queries.productsQueries.softDelete(id = id, deletedAt = java.time.Instant.now().toString())
    }
  }

  override suspend fun adjustStock(productId: Long, delta: Stock, reason: String) {
    queries.productsQueries.transaction(true) {
      val current = queries.productsQueries.findById(id = productId).executeAsList().firstOrNull()!!
      val newStock = (current.current_stock ?: 0.0).toStockRounded().plus(delta)
      queries.productsQueries.update(
        id = productId,
        categoryId = current.category_id,
        sku = current.sku,
        name = current.name,
        sellingPrice = current.selling_price,
        purchasePrice = current.purchase_price,
        currentStock = newStock.value.toDouble(),
        minStock = current.min_stock,
        imageUri = current.image_uri,
        updatedAt = java.time.Instant.now().toString()
      )
    }
  }

  override fun watchAll(): Flow<List<Product>> = flow {
    emit(queries.productsQueries.findAll().executeAsList().map { mapProduct(it) })
  }

  private fun mapProduct(row: Products): Product {
    return Product(
      id = row.id,
      categoryId = row.category_id,
      sku = row.sku?.let { SKU.create(it) },
      name = row.name,
      sellingPrice = row.selling_price?.toMoneyRounded(Currency.Bs) ?: Money.zero(Currency.Bs),
      purchasePrice = row.purchase_price?.toMoneyRounded(Currency.Bs) ?: Money.zero(Currency.Bs),
      currentStock = row.current_stock?.toStockRounded() ?: Stock(BigDecimal.ZERO),
      minStock = row.min_stock?.toStockRounded() ?: Stock(BigDecimal.ZERO),
      imageUri = row.image_uri,
      registeredAt = row.registered_at,
      updatedAt = row.updated_at,
      deletedAt = row.deleted_at
    )
  }
}