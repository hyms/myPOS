package com.tpvmovilexpress.data.repositories

import com.tpvmovilexpress.domain.entities.CategoryId
import com.tpvmovilexpress.domain.entities.CreateProductInput
import com.tpvmovilexpress.domain.entities.Product
import com.tpvmovilexpress.domain.entities.ProductId
import com.tpvmovilexpress.domain.entities.UpdateProductInput

/**
 * Repository port for [Product] aggregate.
 * Implementations must filter soft-deleted products by default in list queries.
 */
public interface IProductRepository {

    /**
     * Lists products with optional search, category filter, sorting, and pagination.
     * Only returns active (non-deleted) products.
     */
    public fun list(query: ProductQuery): List<ProductWithPopularity>

    /**
     * Finds a product by ID (including soft-deleted ones).
     * Returns null if not found.
     */
    public fun findById(id: ProductId): Product?

    /**
     * Finds an active product by SKU.
     * Returns null if not found or soft-deleted.
     */
    public fun findBySku(sku: String): Product?

    /**
     * Creates a new product.
     * @return The created product with its assigned ID
     * @throws IllegalArgumentException if category doesn't exist or SKU is duplicated
     */
    public fun create(input: CreateProductInput): Product

    /**
     * Updates an existing product.
     * @return The updated product
     * @throws IllegalArgumentException if product doesn't exist or SKU is duplicated
     */
    public fun update(id: ProductId, input: UpdateProductInput): Product

    /**
     * Soft-deletes a product by setting `deleted_at`.
     * Preserves historical data for transaction references.
     */
    public fun delete(id: ProductId)

    /**
     * Finds multiple products by their IDs.
     * Returns only active (non-deleted) products.
     */
    public fun findByIds(ids: List<ProductId>): List<Product>
}

/**
 * Query parameters for product listing.
 */
public data class ProductQuery(
    public val search: String? = null,
    public val categoryId: CategoryId? = null,
    public val sort: SortOrder = SortOrder.NAME_ASC,
    public val limit: Int = 15,
    public val offset: Int = 0
)

/**
 * Sort order for product listings.
 */
public enum class SortOrder {
    NAME_ASC,
    POPULARITY_DESC
}

/**
 * Product with a popularity score (number of times sold).
 */
public data class ProductWithPopularity(
    public val product: Product,
    public val popularity: Int
)
