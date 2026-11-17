package com.copitoB.mypos.domain.repository

import com.copitoB.mypos.domain.entities.Category

interface CategoryRepository {
  suspend fun create(name: String): Category
  suspend fun findById(id: Long): Category?
  suspend fun list(): List<Category>
  suspend fun update(id: Long, name: String): Category
  suspend fun softDelete(id: Long)
}