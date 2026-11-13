package com.tpvmovilexpress.domain.entities

import com.tpvmovilexpress.domain.valueobjects.Currency
import com.tpvmovilexpress.domain.valueobjects.Money
import com.tpvmovilexpress.domain.valueobjects.SKU
import com.tpvmovilexpress.domain.valueobjects.Stock

/**
 * Aggregate root representing a product in the catalog.
 * Immutable data class with all English identifiers.
 */
public data class Product(
    public val id: ProductId,
    public val categoryId: CategoryId,
    public val sku: SKU?,
    public val name: String,
    public val sellingPrice: Money,
    public val purchasePrice: Money,
    public val currentStock: Stock,
    public val minStock: Stock,
    public val imageUri: String?,
    public val registeredAt: String,
    public val updatedAt: String,
    public val deletedAt: String?
) {
    public companion object {
        /**
         * Factory method to create a new [Product] from input DTOs.
         * Validates all invariants before construction.
         *
         * @throws IllegalArgumentException if any invariant is violated
         */
        public fun create(input: CreateProductInput, currency: Currency, now: String): Product {
            val trimmedName = input.name.trim()
            require(trimmedName.isNotEmpty()) { "Product name cannot be empty" }

            val sellingPrice = Money.of(input.sellingPrice, currency)
            require(sellingPrice.amountCents >= 0) { "Selling price cannot be negative" }

            val purchasePrice = Money.of(input.purchasePrice, currency)
            require(purchasePrice.amountCents >= 0) { "Purchase price cannot be negative" }

            val currentStock = Stock.of(input.currentStock)
            val minStock = Stock.of(input.minStock)

            val sku = input.sku?.let { raw ->
                if (raw.trim().isEmpty()) null else SKU.create(raw)
            }

            return Product(
                id = ProductId(0),
                categoryId = input.categoryId,
                sku = sku,
                name = trimmedName,
                sellingPrice = sellingPrice,
                purchasePrice = purchasePrice,
                currentStock = currentStock,
                minStock = minStock,
                imageUri = input.imageUri,
                registeredAt = now,
                updatedAt = now,
                deletedAt = null
            )
        }
    }

    /**
     * Returns true if the product's current stock is below its minimum threshold.
     */
    public val isLowStock: Boolean
        get() = currentStock.isBelow(minStock)

    /**
     * Returns true if the product has been soft-deleted.
     */
    public val isDeleted: Boolean
        get() = deletedAt != null
}

/**
 * Lightweight summary projection of a product for list/grid displays.
 */
public data class ProductSummary(
    public val id: ProductId,
    public val name: String,
    public val sellingPrice: Money,
    public val purchasePrice: Money,
    public val currentStock: Stock,
    public val imageUri: String?,
    public val categoryId: CategoryId,
    public val sku: SKU?
)

/**
 * DTO for creating a new product.
 */
public data class CreateProductInput(
    public val categoryId: CategoryId,
    public val sku: String?,
    public val name: String,
    public val sellingPrice: Double,
    public val purchasePrice: Double,
    public val currentStock: Double,
    public val minStock: Double,
    public val imageUri: String?
)

/**
 * DTO for updating an existing product. Same fields as create.
 */
public typealias UpdateProductInput = CreateProductInput
