package com.tpvmovilexpress.data.repositories

import com.tpvmovilexpress.domain.entities.Category
import com.tpvmovilexpress.domain.entities.CategoryId
import com.tpvmovilexpress.domain.entities.CreateCategoryInput

/**
 * Repository port for [Category] aggregate.
 * Implementations must filter soft-deleted categories by default.
 */
public interface ICategoryRepository {
    /**
     * Returns all active (non-deleted) categories.
     */
    public fun list(): List<Category>

    /**
     * Finds a category by ID, including soft-deleted ones.
     * Returns null if not found.
     */
    public fun findById(id: CategoryId): Category?

    /**
     * Creates a new category.
     * @return The created category with its assigned ID
     */
    public fun create(input: CreateCategoryInput): Category
}
