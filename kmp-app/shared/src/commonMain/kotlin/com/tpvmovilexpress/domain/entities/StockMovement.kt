package com.tpvmovilexpress.domain.entities

import com.tpvmovilexpress.domain.valueobjects.Stock

/**
 * Sealed class representing the type of stock movement.
 */
public sealed class StockMovementType(public val code: String) {
    /** Stock received (purchase or adjustment in). */
    public object IN : StockMovementType("IN")

    /** Stock dispatched (sale or adjustment out). */
    public object OUT : StockMovementType("OUT")

    /** Manual stock correction (inventory audit). */
    public object ADJUSTMENT : StockMovementType("ADJUSTMENT")

    public companion object {
        private val BY_CODE: Map<String, StockMovementType> = mapOf(
            "IN" to IN,
            "OUT" to OUT,
            "ADJUSTMENT" to ADJUSTMENT
        )

        /**
         * Creates a [StockMovementType] from a code string.
         * @throws IllegalArgumentException if code is not recognized
         */
        public fun fromCode(code: String): StockMovementType =
            BY_CODE[code.uppercase()]
                ?: throw IllegalArgumentException("Invalid stock movement type: $code")
    }
}

/**
 * Entity representing a stock movement record.
 * Tracks all stock changes for audit and history purposes.
 */
public data class StockMovement(
    public val id: StockMovementId,
    public val productId: ProductId,
    public val type: StockMovementType,
    public val quantity: Stock,
    public val sourceTransactionId: TransactionId?,
    public val note: String?,
    public val registeredAt: String
) {
    public companion object {
        /**
         * Factory method to create a new [StockMovement].
         *
         * @throws IllegalArgumentException if quantity is zero
         */
        public fun create(
            productId: ProductId,
            type: StockMovementType,
            quantity: Stock,
            sourceTransactionId: TransactionId? = null,
            note: String? = null,
            now: String
        ): StockMovement {
            require(!quantity.isZero) { "Stock movement quantity cannot be zero" }
            return StockMovement(
                id = StockMovementId(0),
                productId = productId,
                type = type,
                quantity = quantity,
                sourceTransactionId = sourceTransactionId,
                note = note,
                registeredAt = now
            )
        }
    }
}
