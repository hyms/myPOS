package com.tpvmovilexpress.data.repositories.impl

import com.tpvmovilexpress.data.database.TPVDatabase
import com.tpvmovilexpress.data.repositories.CreateProductInput
import com.tpvmovilexpress.data.repositories.IProductRepository
import com.tpvmovilexpress.data.repositories.ProductQuery
import com.tpvmovilexpress.data.repositories.ProductWithPopularity
import com.tpvmovilexpress.data.repositories.SortOrder
import com.tpvmovilexpress.data.repositories.UpdateProductInput
import com.tpvmovilexpress.domain.entities.CategoryId
import com.tpvmovilexpress.domain.entities.Product
import com.tpvmovilexpress.domain.entities.ProductId
import com.tpvmovilexpress.domain.valueobjects.Currency
import com.tpvmovilexpress.domain.valueobjects.Money
import com.tpvmovilexpress.domain.valueobjects.SKU
import com.tpvmovilexpress.domain.valueobjects.Stock
import kotlinx.datetime.Clock

/**
 * SQLDelight implementation of [IProductRepository].
 * All write operations are wrapped in atomic transactions.
 */
public class SqlDelightProductRepository(
    private val database: TPVDatabase,
    private val currency: Currency = Currency.default()
) : IProductRepository {

    private val queries get() = database.productsQueries

    override fun list(query: ProductQuery): List<ProductWithPopularity> {
        val searchPattern = query.search?.let { "%$it%" }
        val hasSearch = searchPattern != null
        val hasCategory = query.categoryId != null
        val byPopularity = query.sort == SortOrder.POPULARITY_DESC

        val rows = when {
            hasSearch && hasCategory && byPopularity -> {
                queries.selectAllWithPopularitySearchByCategory(
                    searchPattern = searchPattern!!,
                    categoryId = query.categoryId!!.value,
                    limit = query.limit.toLong(),
                    offset = query.offset.toLong()
                ).executeAsList()
            }
            hasSearch && hasCategory -> {
                queries.selectAllWithPopularitySearchByCategory(
                    searchPattern = searchPattern!!,
                    categoryId = query.categoryId!!.value,
                    limit = query.limit.toLong(),
                    offset = query.offset.toLong()
                ).executeAsList()
            }
            hasSearch && byPopularity -> {
                queries.selectAllWithPopularityByPopularity(
                    limit = query.limit.toLong(),
                    offset = query.offset.toLong()
                ).executeAsList().filter {
                    it.name.contains(query.search!!, ignoreCase = true)
                }
            }
            hasSearch -> {
                queries.selectAllWithPopularitySearch(
                    searchPattern = searchPattern!!,
                    limit = query.limit.toLong(),
                    offset = query.offset.toLong()
                ).executeAsList()
            }
            hasCategory && byPopularity -> {
                queries.selectAllWithPopularityByCategory(
                    categoryId = query.categoryId!!.value,
                    limit = query.limit.toLong(),
                    offset = query.offset.toLong()
                ).executeAsList()
            }
            hasCategory -> {
                queries.selectAllWithPopularityByCategory(
                    categoryId = query.categoryId!!.value,
                    limit = query.limit.toLong(),
                    offset = query.offset.toLong()
                ).executeAsList()
            }
            byPopularity -> {
                queries.selectAllWithPopularityByPopularity(
                    limit = query.limit.toLong(),
                    offset = query.offset.toLong()
                ).executeAsList()
            }
            else -> {
                queries.selectAllWithPopularity(
                    limit = query.limit.toLong(),
                    offset = query.offset.toLong()
                ).executeAsList()
            }
        }

        return rows.map { row ->
            ProductWithPopularity(
                product = row.toProduct(),
                popularity = row.popularity.toInt()
            )
        }
    }

    override fun findById(id: ProductId): Product? =
        queries.selectById(id.value).executeAsOneOrNull()?.toProduct()

    override fun findBySku(sku: String): Product? =
        queries.selectBySku(sku.uppercase().trim()).executeAsOneOrNull()?.toProduct()

    override fun create(input: CreateProductInput): Product {
        val trimmedName = input.name.trim()
        require(trimmedName.isNotEmpty()) { "Product name cannot be empty" }

        val sellingPrice = input.sellingPrice
        require(sellingPrice >= 0) { "Selling price cannot be negative" }

        val purchasePrice = input.purchasePrice
        require(purchasePrice >= 0) { "Purchase price cannot be negative" }

        val currentStock = input.currentStock
        require(currentStock >= 0) { "Current stock cannot be negative" }

        val minStock = input.minStock
        require(minStock >= 0) { "Minimum stock cannot be negative" }

        val normalizedSku = input.sku?.trim()?.uppercase()?.takeIf { it.isNotEmpty() }

        if (normalizedSku != null) {
            val exists = queries.existsBySkuExcludingId(
                sku = normalizedSku,
                excludeId = -1L
            ).executeAsOne()
            require(exists == 0L) { "SKU '$normalizedSku' already exists" }
        }

        val nowEpochMillis = Clock.System.now().toEpochMilliseconds()

        return database.transactionWithResult {
            queries.insert(
                categoryId = input.categoryId.value,
                sku = normalizedSku,
                name = trimmedName,
                sellingPrice = sellingPrice,
                purchasePrice = purchasePrice,
                currentStock = currentStock,
                minStock = minStock,
                imageUri = input.imageUri,
                registeredAt = nowEpochMillis,
                updatedAt = nowEpochMillis
            )

            val newId = queries.lastInsertId().executeAsOne()

            Product(
                id = ProductId(newId),
                categoryId = input.categoryId,
                sku = normalizedSku?.let { SKU.createNormalized(it) },
                name = trimmedName,
                sellingPrice = Money.of(sellingPrice, currency),
                purchasePrice = Money.of(purchasePrice, currency),
                currentStock = Stock.of(currentStock),
                minStock = Stock.of(minStock),
                imageUri = input.imageUri,
                registeredAt = epochMillisToIso(nowEpochMillis),
                updatedAt = epochMillisToIso(nowEpochMillis),
                deletedAt = null
            )
        }
    }

    override fun update(id: ProductId, input: UpdateProductInput): Product {
        val existing = queries.selectById(id.value).executeAsOneOrNull()
            ?: throw IllegalArgumentException("Product not found: ${id.value}")
        require(existing.deleted_at == null) { "Cannot update a deleted product" }

        val trimmedName = input.name.trim()
        require(trimmedName.isNotEmpty()) { "Product name cannot be empty" }

        require(input.sellingPrice >= 0) { "Selling price cannot be negative" }
        require(input.purchasePrice >= 0) { "Purchase price cannot be negative" }
        require(input.currentStock >= 0) { "Current stock cannot be negative" }
        require(input.minStock >= 0) { "Minimum stock cannot be negative" }

        val normalizedSku = input.sku?.trim()?.uppercase()?.takeIf { it.isNotEmpty() }

        if (normalizedSku != null) {
            val exists = queries.existsBySkuExcludingId(
                sku = normalizedSku,
                excludeId = id.value
            ).executeAsOne()
            require(exists == 0L) { "SKU '$normalizedSku' already exists" }
        }

        val nowEpochMillis = Clock.System.now().toEpochMilliseconds()

        return database.transactionWithResult {
            queries.update(
                id = id.value,
                categoryId = input.categoryId.value,
                sku = normalizedSku,
                name = trimmedName,
                sellingPrice = input.sellingPrice,
                purchasePrice = input.purchasePrice,
                currentStock = input.currentStock,
                minStock = input.minStock,
                imageUri = input.imageUri,
                updatedAt = nowEpochMillis
            )

            Product(
                id = id,
                categoryId = input.categoryId,
                sku = normalizedSku?.let { SKU.createNormalized(it) },
                name = trimmedName,
                sellingPrice = Money.of(input.sellingPrice, currency),
                purchasePrice = Money.of(input.purchasePrice, currency),
                currentStock = Stock.of(input.currentStock),
                minStock = Stock.of(input.minStock),
                imageUri = input.imageUri,
                registeredAt = epochMillisToIso(existing.registered_at),
                updatedAt = epochMillisToIso(nowEpochMillis),
                deletedAt = null
            )
        }
    }

    override fun delete(id: ProductId) {
        val nowEpochMillis = Clock.System.now().toEpochMilliseconds()
        val affected = queries.softDelete(
            id = id.value,
            deletedAt = nowEpochMillis
        )
        // If no rows affected, product doesn't exist or already deleted - silent no-op
    }

    override fun findByIds(ids: List<ProductId>): List<Product> {
        if (ids.isEmpty()) return emptyList()
        return queries.selectByIds(ids.map { it.value }).executeAsList().map { it.toProduct() }
    }

    private fun epochMillisToIso(epochMillis: Long): String =
        kotlinx.datetime.Instant.fromEpochMilliseconds(epochMillis).toString()

    private fun com.tpvmovilexpress.data.database.SelectAllWithPopularity.toProduct(): Product =
        Product(
            id = ProductId(this.id),
            categoryId = CategoryId(this.category_id),
            sku = this.sku?.let { SKU.createNormalized(it) },
            name = this.name,
            sellingPrice = Money.of(this.selling_price, currency),
            purchasePrice = Money.of(this.purchase_price, currency),
            currentStock = Stock.of(this.current_stock),
            minStock = Stock.of(this.min_stock),
            imageUri = this.image_uri,
            registeredAt = epochMillisToIso(this.registered_at),
            updatedAt = epochMillisToIso(this.updated_at),
            deletedAt = this.deleted_at?.let { epochMillisToIso(it) }
        )

    private fun com.tpvmovilexpress.data.database.Products.toProduct(): Product =
        Product(
            id = ProductId(this.id),
            categoryId = CategoryId(this.category_id),
            sku = this.sku?.let { SKU.createNormalized(it) },
            name = this.name,
            sellingPrice = Money.of(this.selling_price, currency),
            purchasePrice = Money.of(this.purchase_price, currency),
            currentStock = Stock.of(this.current_stock),
            minStock = Stock.of(this.min_stock),
            imageUri = this.image_uri,
            registeredAt = epochMillisToIso(this.registered_at),
            updatedAt = epochMillisToIso(this.updated_at),
            deletedAt = this.deleted_at?.let { epochMillisToIso(it) }
        )
}
