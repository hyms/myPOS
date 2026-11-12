package com.tpvmovilexpress.domain.valueobjects

/**
 * Sealed class representing transaction types.
 * Ensures exhaustive when expressions and type safety.
 */
sealed class TransactionType(val code: String, val displayName: String) {
    object SALE : TransactionType("SALE", "Venta")
    object PURCHASE : TransactionType("PURCHASE", "Compra")
    object EXPENSE : TransactionType("EXPENSE", "Gasto")

    companion object {
        private val BY_CODE = mapOf(
            "SALE" to SALE,
            "PURCHASE" to PURCHASE,
            "EXPENSE" to EXPENSE
        )

        /**
         * Creates a [TransactionType] from a code string.
         * @throws IllegalArgumentException if code is not recognized
         */
        fun fromCode(code: String): TransactionType =
            BY_CODE[code.uppercase()] ?: throw IllegalArgumentException("Invalid transaction type: $code")

        /**
         * Returns all available transaction types.
         */
        val all: List<TransactionType> = listOf(SALE, PURCHASE, EXPENSE)
    }

    /**
     * Checks if this transaction type affects stock.
     * SALE decreases stock, PURCHASE increases stock, EXPENSE doesn't affect stock.
     */
    val affectsStock: Boolean
        get() = this != EXPENSE

    /**
     * Returns the stock adjustment direction for this transaction type.
     * Positive for PURCHASE (stock in), negative for SALE (stock out), zero for EXPENSE.
     */
    val stockDirection: Int
        get() = when (this) {
            SALE -> -1
            PURCHASE -> 1
            EXPENSE -> 0
        }

    /**
     * Returns the accumulator field name for this transaction type.
     */
    val accumulatorField: String
        get() = when (this) {
            SALE -> "total_sales"
            PURCHASE -> "total_purchases"
            EXPENSE -> "total_expenses"
        }

    /**
     * Returns the balance adjustment direction for this transaction type.
     * Positive for SALE (income), negative for PURCHASE/EXPENSE (outflow).
     */
    val balanceDirection: Int
        get() = when (this) {
            SALE -> 1
            PURCHASE -> -1
            EXPENSE -> -1
        }

    override fun toString(): String = displayName
}