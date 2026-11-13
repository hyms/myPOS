package com.tpvmovilexpress.data.repositories.impl

import com.tpvmovilexpress.data.database.TPVDatabase
import com.tpvmovilexpress.data.repositories.ICategoryRepository
import com.tpvmovilexpress.data.repositories.CreateCategoryInput as DomainCreateCategoryInput
import com.tpvmovilexpress.domain.entities.Category
import com.tpvmovilexpress.domain.entities.CategoryId
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

/**
 * SQLDelight implementation of [ICategoryRepository].
 */
public class SqlDelightCategoryRepository(
    private val database: TPVDatabase
) : ICategoryRepository {

    private val queries get() = database.categoriesQueries

    override fun list(): List<Category> =
        queries.selectAllActive().executeAsList().map { it.toDomain() }

    override fun findById(id: CategoryId): Category? =
        queries.selectById(id.value).executeAsOneOrNull()?.toDomain()

    override fun create(input: DomainCreateCategoryInput): Category {
        val trimmedName = input.name.trim()
        require(trimmedName.isNotEmpty()) { "Category name cannot be empty" }

        val nowEpochMillis = Clock.System.now().toEpochMilliseconds()

        queries.insert(
            name = trimmedName,
            registeredAt = nowEpochMillis
        )

        val newId = queries.lastInsertId().executeAsOne()

        return Category(
            id = CategoryId(newId),
            name = trimmedName,
            registeredAt = epochMillisToIso(nowEpochMillis),
            deletedAt = null
        )
    }

    private fun epochMillisToIso(epochMillis: Long): String {
        val instant = kotlinx.datetime.Instant.fromEpochMilliseconds(epochMillis)
        return instant.toString()
    }

    private fun com.tpvmovilexpress.data.database.Categories.toDomain(): Category =
        Category(
            id = CategoryId(this.id),
            name = this.name,
            registeredAt = epochMillisToIso(this.registered_at),
            deletedAt = this.deleted_at?.let { epochMillisToIso(it) }
        )
}
