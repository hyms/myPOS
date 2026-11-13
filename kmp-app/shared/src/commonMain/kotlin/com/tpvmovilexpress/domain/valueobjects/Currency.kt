package com.tpvmovilexpress.domain.valueobjects

import java.text.NumberFormat
import java.util.Currency
import java.util.Locale

/**
 * Supported currencies for the application.
 * Uses ISO 4217 currency codes internally.
 */
public enum class CurrencyCode {
    BOB,
    USD,
    EUR,
    PEN
}

/**
 * Currency configuration with formatting capabilities.
 * Immutable value object containing locale-specific formatting.
 */
public data class Currency(
    public val code: CurrencyCode,
    public val symbol: String,
    public val locale: Locale,
    public val fractionDigits: Int = 2
) {
    public companion object {
        private val REGISTRY: Map<CurrencyCode, Currency> = mapOf(
            CurrencyCode.BOB to Currency(
                code = CurrencyCode.BOB,
                symbol = "Bs.",
                locale = Locale("es", "BO"),
                fractionDigits = 2
            ),
            CurrencyCode.USD to Currency(
                code = CurrencyCode.USD,
                symbol = "$",
                locale = Locale("en", "US"),
                fractionDigits = 2
            ),
            CurrencyCode.EUR to Currency(
                code = CurrencyCode.EUR,
                symbol = "€",
                locale = Locale("es", "ES"),
                fractionDigits = 2
            ),
            CurrencyCode.PEN to Currency(
                code = CurrencyCode.PEN,
                symbol = "S/",
                locale = Locale("es", "PE"),
                fractionDigits = 2
            )
        )

        /**
         * Returns the [Currency] configuration for the given [CurrencyCode].
         * @throws IllegalArgumentException if the currency code is not supported
         */
        public fun get(code: CurrencyCode): Currency =
            REGISTRY[code] ?: throw IllegalArgumentException("Unsupported currency: $code")

        /**
         * Returns the default currency (Bolivianos).
         */
        public fun default(): Currency = get(CurrencyCode.BOB)
    }

    /**
     * Creates a [NumberFormat] configured for this currency.
     */
    public fun formatter(): NumberFormat =
        NumberFormat.getCurrencyInstance(locale).apply {
            currency = Currency.getInstance(code.name)
            minimumFractionDigits = fractionDigits
            maximumFractionDigits = fractionDigits
        }

    /**
     * Formats a [Money] amount using this currency's formatter.
     */
    public fun format(amount: Money): String = formatter().format(amount.amount)
}
