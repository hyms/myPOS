package com.tpvmovilexpress.domain.valueobjects

import java.math.BigDecimal
import java.math.RoundingMode

/**
 * Immutable value object representing stock quantity.
 * Enforces non-negative fractional stock up to 3 decimal places.
 *
 * @param amountMilliunits Amount in thousandths (milliunits), e.g., 123450 = 123.450
 */
@JvmInline
value class Stock private constructor(
    val amountMilliunits: Long
) : Comparable<Stock> {

    companion object {
        /** Scale factor for 3 decimal places (1000) */
        private const val SCALE = 1000L

        /**
         * Creates a [Stock] instance from a decimal amount (e.g., 123.456).
         * @throws IllegalArgumentException if amount is negative or not finite
         */
        fun of(amount: Double): Stock {
            require(amount.isFinite()) { "Stock amount must be finite" }
            require(amount >= 0.0) { "Stock cannot be negative" }
            val milliunits = (amount * SCALE).roundToLong()
            return Stock(milliunits)
        }

        /**
         * Creates a [Stock] instance from milliunits (thousandths).
         * @throws IllegalArgumentException if milliunits is negative
         */
        fun fromMilliunits(milliunits: Long): Stock {
            require(milliunits >= 0) { "Stock cannot be negative" }
            return Stock(milliunits)
        }

        /**
         * Creates a zero [Stock] instance.
         */
        fun zero(): Stock = Stock(0)

        /**
         * Parses a string amount (e.g., "123.456") into a [Stock] instance.
         * @throws NumberFormatException if the string is not a valid number
         */
        fun parse(amountStr: String): Stock {
            val bd = BigDecimal(amountStr)
            val milliunits = bd.scaleByPowerOfTen(3).toLongExact()
            require(milliunits >= 0) { "Stock cannot be negative" }
            return Stock(milliunits)
        }
    }

    /**
     * Returns the amount in decimal units (e.g., 123.456).
     */
    val amount: Double
        get() = amountMilliunits.toDouble() / SCALE

    /**
     * Returns the amount in milliunits (thousandths).
     */
    val milliunits: Long
        get() = amountMilliunits

    /**
     * Checks if the stock is zero.
     */
    val isZero: Boolean
        get() = amountMilliunits == 0L

    /**
     * Adds another [Stock] quantity.
     */
    operator fun plus(other: Stock): Stock = Stock(this.amountMilliunits + other.amountMilliunits)

    /**
     * Subtracts another [Stock] quantity.
     * @throws IllegalArgumentException if result would be negative
     */
    operator fun minus(other: Stock): Stock {
        val result = this.amountMilliunits - other.amountMilliunits
        require(result >= 0) { "Stock cannot go negative: $this - $other" }
        return Stock(result)
    }

    /**
     * Checks if this stock is below a minimum threshold.
     */
    fun isBelow(min: Stock): Boolean = this.amountMilliunits < min.amountMilliunits

    /**
     * Returns the maximum of this stock and another.
     */
    fun max(other: Stock): Stock = if (this >= other) this else other

    /**
     * Returns the minimum of this stock and another.
     */
    fun min(other: Stock): Stock = if (this <= other) this else other

    override fun compareTo(other: Stock): Int = amountMilliunits.compareTo(other.amountMilliunits)

    override fun toString(): String = amount.toString()
}

/**
 * Extension functions for easier Stock creation.
 */
inline fun Number.toStock(): Stock = Stock.of(this.toDouble())