package com.tpvmovilexpress.domain.entities

/**
 * Branded type for Product ID.
 * Uses @JvmInline value class for zero-overhead wrapper.
 */
@JvmInline
public value class ProductId(public val value: Long) : Comparable<ProductId> {
    override fun compareTo(other: ProductId): Int = value.compareTo(other.value)
    override fun toString(): String = value.toString()
}

/**
 * Branded type for Category ID.
 */
@JvmInline
public value class CategoryId(public val value: Long) : Comparable<CategoryId> {
    override fun compareTo(other: CategoryId): Int = value.compareTo(other.value)
    override fun toString(): String = value.toString()
}

/**
 * Branded type for Expense Category ID.
 */
@JvmInline
public value class ExpenseCategoryId(public val value: Long) : Comparable<ExpenseCategoryId> {
    override fun compareTo(other: ExpenseCategoryId): Int = value.compareTo(other.value)
    override fun toString(): String = value.toString()
}

/**
 * Branded type for Transaction ID.
 */
@JvmInline
public value class TransactionId(public val value: Long) : Comparable<TransactionId> {
    override fun compareTo(other: TransactionId): Int = value.compareTo(other.value)
    override fun toString(): String = value.toString()
}

/**
 * Branded type for Stock Movement ID.
 */
@JvmInline
public value class StockMovementId(public val value: Long) : Comparable<StockMovementId> {
    override fun compareTo(other: StockMovementId): Int = value.compareTo(other.value)
    override fun toString(): String = value.toString()
}

/**
 * Branded type for Settings ID (singleton, always 1).
 */
@JvmInline
public value class SettingsId(public val value: Long) {
    public companion object {
        public val SINGLETON: SettingsId = SettingsId(1)
    }
    override fun toString(): String = value.toString()
}
