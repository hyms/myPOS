package com.tpvmovilexpress.domain.valueobjects

/**
 * Immutable value object representing the change amount in a cash transaction.
 * Calculated as `received - total`.
 */
public data class ChangeAmount(
    public val amount: Money
) {
    public companion object {
        /**
         * Creates a zero [ChangeAmount] for the given currency.
         */
        public fun zero(currency: Currency): ChangeAmount = ChangeAmount(Money.zero(currency))

        /**
         * Calculates change from received amount and total.
         */
        public fun calculate(received: Money, total: Money): ChangeAmount {
            require(received.currency.code == total.currency.code) { "Currency mismatch" }
            return ChangeAmount(received - total)
        }
    }

    /**
     * Checks if the change is sufficient (non-negative).
     */
    public val isSufficient: Boolean
        get() = !amount.isNegative

    /**
     * Returns the color theme for UI display.
     */
    public val colorTheme: String
        get() = if (isSufficient) "success" else "danger"

    /**
     * Returns the absolute shortfall if insufficient, or the change if sufficient.
     */
    public val displayAmount: Money
        get() = if (isSufficient) amount else -amount

    /**
     * Formats the change for display with appropriate sign.
     */
    public fun format(): String {
        val formatted = displayAmount.format()
        return if (isSufficient) "+$formatted" else "-$formatted"
    }
}
