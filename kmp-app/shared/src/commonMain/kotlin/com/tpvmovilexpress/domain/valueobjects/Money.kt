package com.tpvmovilexpress.domain.valueobjects

import java.math.BigDecimal
import java.math.RoundingMode

/**
 * Immutable value object representing a monetary amount with currency.
 * Uses scaled integer arithmetic (cents) to prevent floating-point drift.
 * Supports negative values for change calculations and refunds.
 *
 * @param amountCents Amount in minor units (cents), e.g., 12345 = 123.45
 * @param currency Currency configuration
 */
@JvmInline
public value class Money private constructor(
    public val amountCents: Long,
    public val currency: Currency
) : Comparable<Money> {

    public companion object {
        private const val SCALE = 100L

        /**
         * Creates a [Money] instance from a major unit amount (e.g., 123.45).
         * @throws IllegalArgumentException if amount is not finite
         */
        public fun of(amount: Double, currency: Currency): Money {
            require(amount.isFinite()) { "Amount must be finite" }
            val cents = (amount * SCALE).roundToLong()
            return Money(cents, currency)
        }

        /**
         * Creates a [Money] instance from minor units (cents).
         */
        public fun fromCents(cents: Long, currency: Currency): Money = Money(cents, currency)

        /**
         * Creates a zero [Money] instance for the given currency.
         */
        public fun zero(currency: Currency): Money = Money(0, currency)

        /**
         * Parses a string amount (e.g., "123.45") into a [Money] instance.
         * @throws NumberFormatException if the string is not a valid number
         */
        public fun parse(amountStr: String, currency: Currency): Money {
            val bd = BigDecimal(amountStr)
            val cents = bd.scaleByPowerOfTen(currency.fractionDigits).toLongExact()
            return Money(cents, currency)
        }
    }

    /**
     * Returns the amount in major units (e.g., 123.45).
     */
    public val amount: Double
        get() = amountCents.toDouble() / SCALE

    /**
     * Returns the amount in minor units (cents).
     */
    public val minorUnits: Long
        get() = amountCents

    /**
     * Checks if the amount is zero.
     */
    public val isZero: Boolean
        get() = amountCents == 0L

    /**
     * Checks if the amount is negative.
     */
    public val isNegative: Boolean
        get() = amountCents < 0L

    /**
     * Checks if the amount is positive.
     */
    public val isPositive: Boolean
        get() = amountCents > 0L

    /**
     * Adds another [Money] amount.
     * @throws IllegalArgumentException if currencies don't match
     */
    public operator fun plus(other: Money): Money {
        require(this.currency.code == other.currency.code) { "Currency mismatch: ${this.currency.code} vs ${other.currency.code}" }
        return Money(this.amountCents + other.amountCents, this.currency)
    }

    /**
     * Subtracts another [Money] amount.
     * @throws IllegalArgumentException if currencies don't match
     */
    public operator fun minus(other: Money): Money {
        require(this.currency.code == other.currency.code) { "Currency mismatch: ${this.currency.code} vs ${other.currency.code}" }
        return Money(this.amountCents - other.amountCents, this.currency)
    }

    /**
     * Multiplies the amount by a factor.
     * Uses [RoundingMode.HALF_EVEN] for banker's rounding.
     */
    public fun times(factor: Double): Money {
        require(factor.isFinite()) { "Factor must be finite" }
        val bd = BigDecimal(amountCents).multiply(BigDecimal.valueOf(factor))
        val rounded = bd.setScale(0, RoundingMode.HALF_EVEN).toLongExact()
        return Money(rounded, this.currency)
    }

    /**
     * Divides the amount by a divisor.
     * Uses [RoundingMode.HALF_EVEN] for banker's rounding.
     * @throws ArithmeticException if divisor is zero
     */
    public fun div(divisor: Double): Money {
        require(divisor.isFinite()) { "Divisor must be finite" }
        require(divisor != 0.0) { "Divisor cannot be zero" }
        val bd = BigDecimal(amountCents).divide(BigDecimal.valueOf(divisor), 0, RoundingMode.HALF_EVEN)
        return Money(bd.toLongExact(), this.currency)
    }

    /**
     * Returns the absolute value.
     */
    public fun abs(): Money = Money(amountCents.absoluteValue, currency)

    /**
     * Returns the negated value.
     */
    public operator fun unaryMinus(): Money = Money(-amountCents, currency)

    /**
     * Rounds to 2 decimal places (no-op for Money, included for API consistency).
     */
    public fun round2(): Money = this

    /**
     * Formats the amount using the currency's formatter.
     */
    public fun format(): String = currency.format(this)

    /**
     * Compares this amount to another.
     * @throws IllegalArgumentException if currencies don't match
     */
    override fun compareTo(other: Money): Int {
        require(this.currency.code == other.currency.code) { "Currency mismatch: ${this.currency.code} vs ${other.currency.code}" }
        return amountCents.compareTo(other.amountCents)
    }

    override fun toString(): String = "${currency.symbol} ${format()}"
}

/**
 * Extension functions for easier Money creation.
 */
public inline fun Number.toMoney(currency: Currency): Money = Money.of(this.toDouble(), currency)
