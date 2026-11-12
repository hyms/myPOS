package com.tpvmovilexpress.domain.rules

import com.tpvmovilexpress.domain.entities.CartType
import com.tpvmovilexpress.domain.valueobjects.Currency
import com.tpvmovilexpress.domain.valueobjects.Money

/**
 * Domain rules for cart calculations.
 */
object CartRules {

    /**
     * Represents a single line in a cart for calculation purposes.
     */
    data class CartLine(
        val unitPrice: Money,
        val quantity: Double,
        val discount: Money
    )

    /**
     * Result of cart totals calculation.
     */
    data class CartTotals(
        val subtotal: Money,
        val discount: Money,
        val total: Money
    )

    /**
     * Selects the appropriate unit price based on cart type.
     */
    fun selectUnitPrice(salePrice: Money, purchasePrice: Money, cartType: CartType): Money =
        when (cartType) {
            CartType.SALE -> salePrice
            CartType.PURCHASE -> purchasePrice
        }

    /**
     * Calculates line subtotal (unitPrice * quantity) rounded to 2 decimal places.
     */
    fun lineSubtotal(line: CartLine): Money {
        val factor = line.quantity
        require(factor.isFinite() && factor >= 0) { "Quantity must be non-negative and finite" }
        return line.unitPrice.times(factor).round2()
    }

    /**
     * Calculates cart totals from lines.
     */
    fun calcTotals(lines: List<CartLine>, currency: Currency): CartTotals {
        require(lines.isNotEmpty()) { "Cart cannot be empty" }

        val currencyCode = lines.first().unitPrice.currency.code
        require(lines.all { it.unitPrice.currency.code == currencyCode }) { "All lines must have same currency" }
        require(currency.code == currencyCode) { "Currency mismatch" }

        val subtotal = lines.fold(Money.zero(currency)) { acc, line ->
            acc + lineSubtotal(line)
        }.round2()

        val discount = lines.fold(Money.zero(currency)) { acc, line ->
            acc + line.discount
        }.round2()

        val clampedDiscount = applyDiscount(subtotal, discount)
        val total = (subtotal - clampedDiscount).round2()

        require(total.amountCents >= 0) { "Total cannot be negative after discount" }

        return CartTotals(subtotal, clampedDiscount, total)
    }

    /**
     * Applies discount, clamping to [0, subtotal].
     */
    fun applyDiscount(subtotal: Money, discount: Money): Money {
        val clamped = discount.amountCents.coerceIn(0L, subtotal.amountCents)
        return Money.fromCents(clamped, subtotal.currency)
    }

    /**
     * Calculates change amount for cash payments.
     */
    fun calcChange(total: Money, received: Money): Money {
        require(total.currency.code == received.currency.code) { "Currency mismatch" }
        return (received - total).round2()
    }
}

/**
 * Represents the type of cart (sale or purchase).
 */
sealed class CartType {
    object SALE : CartType()
    object PURCHASE : CartType()
}