package com.tpvmovilexpress.domain.entities

/**
 * Aggregate root representing a product category.
 * Immutable data class with all English identifiers.
 */
public data class Category(
    public val id: CategoryId,
    public val name: String,
    public val registeredAt: String,
    public val deletedAt: String?
) {
    public companion object {
        /**
         * Factory method to create a new [Category] from input.
         *
         * @throws IllegalArgumentException if name is empty
         */
        public fun create(input: CreateCategoryInput, now: String): Category {
            val trimmedName = input.name.trim()
            require(trimmedName.isNotEmpty()) { "Category name cannot be empty" }
            return Category(
                id = CategoryId(0),
                name = trimmedName,
                registeredAt = now,
                deletedAt = null
            )
        }
    }

    /**
     * Returns true if the category has been soft-deleted.
     */
    public val isDeleted: Boolean
        get() = deletedAt != null
}

/**
 * DTO for creating a new category.
 */
public data class CreateCategoryInput(
    public val name: String
)
