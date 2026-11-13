package com.tpvmovilexpress.domain.valueobjects

/**
 * Sealed class representing payment methods.
 * Ensures exhaustive when expressions and type safety.
 */
public sealed class PaymentMethod(public val code: String, public val displayName: String) {
    public object CASH : PaymentMethod("CASH", "Efectivo")
    public object CARD : PaymentMethod("CARD", "Tarjeta")
    public object TRANSFER : PaymentMethod("TRANSFER", "Transferencia")
    public object QR : PaymentMethod("QR", "QR")

    public companion object {
        private val BY_CODE = mapOf(
            "CASH" to CASH,
            "CARD" to CARD,
            "TRANSFER" to TRANSFER,
            "QR" to QR
        )

        /**
         * Creates a [PaymentMethod] from a code string.
         * @throws IllegalArgumentException if code is not recognized
         */
        public fun fromCode(code: String): PaymentMethod =
            BY_CODE[code.uppercase()] ?: throw IllegalArgumentException("Invalid payment method: $code")

        /**
         * Returns all available payment methods.
         */
        public val all: List<PaymentMethod> = listOf(CASH, CARD, TRANSFER, QR)
    }

    /**
     * Checks if this is a cash payment method.
     */
    public val isCash: Boolean
        get() = this == CASH

    override fun toString(): String = displayName
}
