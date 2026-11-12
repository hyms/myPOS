package com.tpvmovilexpress.domain.entities

import kotlinx.serialization.Serializable

/**
 * Branded type for Product ID.
 * Uses @JvmInline value class for zero-overhead wrapper.
 */
@JvmInline
value class ProductId(val value: Long) : Comparable<ProductId> {
    override fun compareTo(other: ProductId): Int = value.compareTo(other.value)
    override fun toString(): String = value.toString()
}

/**
 * Branded type for Category ID.
 */
@JvmInline
value class CategoryId(val value: Long) : Comparable<CategoryId> {
    override fun compareTo(other: CategoryId): Int = value.compareTo(other.value)
    override fun toString(): String = value.toString()
}

/**
 * Branded type for Expense Category ID.
 */
@JvmInline
value class ExpenseCategoryId(val value: Long) : Comparable<ExpenseCategoryId> {
    override fun compareTo(other: ExpenseCategoryId): Int = value.compareTo(other.value)
    override fun toString(): String = value.toString()
}

/**
 * Branded type for Transaction ID.
 */
@JvmInline
value class TransactionId(val value: Long) : Comparable<TransactionId> {
    override fun compareTo(other: TransactionId): Int = value.compareTo(other.value)
    override fun toString(): String = value.toString()
}

/**
 * Branded type for Settings ID (singleton, always 1).
 */
@JvmInline
value class SettingsId(val value: Long) {
    companion object {
        val SINGLETON = SettingsId(1)
    }
    override fun toString(): String = value.toString()
}