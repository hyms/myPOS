package com.copitoB.mypos.data.repository

import app.cash.sqldelight.db.SqlDriver
import com.copitoB.mypos.domain.entities.Category
import com.copitoB.mypos.domain.repository.CategoryRepository
import com.copitoB.mypos.data.repository.TpvDatabase

class SqliteCategoryRepository(
  private val driver: SqlDriver
) : CategoryRepository {
  private val queries = TpvDatabase(driver)

  override suspend fun create(name: String): Category {
    var id: Long = 0
    queries.productsQueries.transaction(true) {
      queries.productsQueries.categoriesInsert(
        name = name,
        registeredAt = java.time.Instant.now().toString()
      )
      id = queries.productsQueries.lastInsertIdCategory().executeAsOne()
    }
    return findById(id)!!
  }

  override suspend fun findById(id: Long): Category? {
    return queries.productsQueries.categoriesFindById(id = id).executeAsList()
      .firstOrNull()?.let { mapCategory(it) }
  }

  override suspend fun list(): List<Category> {
    return queries.productsQueries.categoriesFindAll().executeAsList().map { mapCategory(it) }
  }

  override suspend fun update(id: Long, name: String): Category {
    queries.productsQueries.categoriesUpdate(
      id = id,
      name = name,
      updatedAt = java.time.Instant.now().toString()
    )
    return findById(id)!!
  }

  override suspend fun softDelete(id: Long) {
    queries.productsQueries.categoriesSoftDelete(
      id = id,
      deletedAt = java.time.Instant.now().toString()
    )
  }

  private fun mapCategory(row: com.copitoB.mypos.data.repository.Categories): Category {
    return Category(
      id = row.id,
      name = row.name,
      registeredAt = row.registered_at,
      deletedAt = row.deleted_at
    )
  }
}