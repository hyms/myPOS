package com.tpvmovilexpress.domain.valueobjects

/**
 * Sealed class representing transaction types.
 * Ensures exhaustive when expressions and type safety.
 */
public sealed class TransactionType(public val code: String, public val displayName: String) {
    public object SALE : TransactionType("SALE", "Venta")
    public object PURCHASE : TransactionType("PURCHASE", "Compra")
    public object EXPENSE : TransactionType("EXPENSE", "Gasto")

    public companion object {
        private val BY_CODE = mapOf(
            "SALE" to SALE,
            "PURCHASE" to PURCHASE,
            "EXPENSE" to EXPENSE
        )

        /**
         * Creates a [TransactionType] from a code string.
         * @throws IllegalArgumentException if code is not recognized
         */
        public fun fromCode(code: String): TransactionType =
            BY_CODE[code.uppercase()] ?: throw IllegalArgumentException("Invalid transaction type: $code")

        /**
         * Returns all available transaction types.
         */
        public val all: List<TransactionType> = listOf(SALE, PURCHASE, EXPENSE)
    }

    /**
     * Checks if this transaction type affects stock.
     */
    public val affectsStock: Boolean
        get() = this != EXPENSE

    /**
     * Returns the stock adjustment direction for this transaction type.
     */
    public val stockDirection: Int
        get() = when (this) {
            SALE -> -1
            PURCHASE -> 1
            EXPENSE -> 0
        }

    /**
     * Returns the accumulator field name for this transaction type.
     */
    public val accumulatorField: String
        get() = when (this) {
            SALE -> "total_sales"
            PURCHASE -> "total_purchases"
            EXPENSE -> "total_expenses"
        }

    /**
     * Returns the balance adjustment direction for this transaction type.
     */
    public val balanceDirection: Int
        get() = when (this) {
            SALE -> 1
            PURCHASE -> -1
            EXPENSE -> -1
        }

    override fun toString(): String = displayName
}
