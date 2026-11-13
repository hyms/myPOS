package com.tpvmovilexpress.domain.rules

import com.tpvmovilexpress.domain.valueobjects.Money

/**
 * Domain rules for expense validation.
 */
public object ExpenseRules {

    /**
     * Validates that an expense amount is positive.
     * @throws IllegalArgumentException if amount is not positive
     */
    public fun validateAmount(amount: Money): Money {
        require(amount.isPositive) { "Expense amount must be positive, got ${amount.amount}" }
        return amount
    }

    /**
     * Validates that an expense description is not empty.
     * @throws IllegalArgumentException if description is empty
     */
    public fun validateDescription(description: String): String {
        val trimmed = description.trim()
        require(trimmed.isNotEmpty()) { "Expense description cannot be empty" }
        return trimmed
    }

    /**
     * Rounds an amount to 2 decimal places using banker's rounding.
     */
    public fun roundAmount(amount: Double): Double {
        require(amount.isFinite()) { "Amount must be finite" }
        return (amount * 100).roundToInt() / 100.0
    }
}
