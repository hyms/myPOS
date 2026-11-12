package com.tpvmovilexpress.domain.valueobjects

/**
 * Immutable value object representing the change amount in a cash transaction.
 * Calculated as `received - total`.
 *
 * @param amount The change amount (can be negative if insufficient)
 */
data class ChangeAmount(
    val amount: Money
) {
    companion object {
        /**
         * Creates a zero [ChangeAmount] for the given currency.
         */
        fun zero(currency: Currency): ChangeAmount = ChangeAmount(Money.zero(currency))

        /**
         * Calculates change from received amount and total.
         */
        fun calculate(received: Money, total: Money): ChangeAmount {
            require(received.currency.code == total.currency.code) { "Currency mismatch" }
            return ChangeAmount(received - total)
        }
    }

    /**
     * Checks if the change is sufficient (non-negative).
     */
    val isSufficient: Boolean
        get() = !amount.isNegative

    /**
     * Returns the color theme for UI display.
     * "success" for sufficient change (green), "danger" for insufficient (red).
     */
    val colorTheme: String
        get() = if (isSufficient) "success" else "danger"

    /**
     * Returns the absolute shortfall if insufficient, or the change if sufficient.
     */
    val displayAmount: Money
        get() = if (isSufficient) amount else -amount

    /**
     * Formats the change for display with appropriate sign.
     */
    fun format(): String {
        val formatted = displayAmount.format()
        return if (isSufficient) "+$formatted" else "-$formatted"
    }
}