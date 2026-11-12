package com.tpvmovilexpress.domain.valueobjects

/**
 * Sealed class representing payment methods.
 * Ensures exhaustive when expressions and type safety.
 */
sealed class PaymentMethod(val code: String, val displayName: String) {
    object CASH : PaymentMethod("CASH", "Efectivo")
    object CARD : PaymentMethod("CARD", "Tarjeta")
    object TRANSFER : PaymentMethod("TRANSFER", "Transferencia")
    object QR : PaymentMethod("QR", "QR")

    companion object {
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
        fun fromCode(code: String): PaymentMethod =
            BY_CODE[code.uppercase()] ?: throw IllegalArgumentException("Invalid payment method: $code")

        /**
         * Returns all available payment methods.
         */
        val all: List<PaymentMethod> = listOf(CASH, CARD, TRANSFER, QR)
    }

    /**
     * Checks if this is a cash payment method.
     */
    val isCash: Boolean
        get() = this == CASH

    override fun toString(): String = displayName
}