package com.tpvmovilexpress.domain.rules

import com.tpvmovilexpress.domain.entities.Product
import com.tpvmovilexpress.domain.valueobjects.Stock
import com.tpvmovilexpress.domain.valueobjects.Currency
import com.tpvmovilexpress.domain.valueobjects.Money

/**
 * Domain rules for stock management.
 */
object StockRules {

    /**
     * Validates that a product has sufficient stock for a given quantity.
     * @throws InsufficientStockException if stock is insufficient
     */
    @Throws(InsufficientStockException::class)
    public fun validateStockAvailable(product: Product, quantity: Stock): Stock {
        val available = product.currentStock
        val required = quantity
        if (available.isBelow(required)) {
            throw InsufficientStockException(
                productId = product.id,
                productName = product.name,
                available = available,
                requested = required
            )
        }
        return available.minus(required)
    }

    /**
     * Checks if a product's current stock is below its minimum threshold.
     */
    public fun isBelowMin(product: Product): Boolean =
        product.currentStock.isBelow(product.minStock)

    /**
     * Validates that a stock quantity is valid (non-negative).
     * @throws IllegalArgumentException if stock is negative
     */
    public fun validateNonNegative(stock: Stock): Stock {
        if (stock.amountMilliunits < 0) {
            throw IllegalArgumentException("Stock cannot be negative: ${stock.amount}")
        }
        return stock
    }

    /**
     * Validates that a minimum stock level is valid.
     * @throws IllegalArgumentException if min stock is negative
     */
    public fun validateMinStock(minStock: Stock): Stock {
        if (minStock.amountMilliunits < 0) {
            throw IllegalArgumentException("Minimum stock cannot be negative: ${minStock.amount}")
        }
        return minStock
    }

    /**
     * Exception thrown when stock is insufficient for an operation.
     */
    public class InsufficientStockException(
        public val productId: ProductId,
        public val productName: String,
        public val available: Stock,
        public val requested: Stock
    ) : IllegalArgumentException(
        "Insufficient stock for '$productName' (id: ${productId.value}). " +
        "Available: ${available.amount}, Requested: ${requested.amount}"
    )
}