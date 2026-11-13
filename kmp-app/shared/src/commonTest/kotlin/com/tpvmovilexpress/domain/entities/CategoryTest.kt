package com.tpvmovilexpress.domain.entities

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class CategoryTest {

    // ─── Factory Tests ─────────────────────────

    @Test
    fun `create builds valid category from input`() {
        val input = CreateCategoryInput(name = "Bebidas")

        val category = Category.create(input, "2024-06-01T12:00:00Z")

        assertEquals("Bebidas", category.name)
        assertEquals(CategoryId(0), category.id)
        assertEquals("2024-06-01T12:00:00Z", category.registeredAt)
        assertNull(category.deletedAt)
    }

    @Test
    fun `create trims category name`() {
        val input = CreateCategoryInput(name = "  Trimmed  ")

        val category = Category.create(input, "2024-01-01T00:00:00Z")

        assertEquals("Trimmed", category.name)
    }

    @Test
    fun `create throws for empty name`() {
        val input = CreateCategoryInput(name = "")

        assertFailsWith<IllegalArgumentException> {
            Category.create(input, "2024-01-01T00:00:00Z")
        }
    }

    @Test
    fun `create throws for blank name`() {
        val input = CreateCategoryInput(name = "   ")

        assertFailsWith<IllegalArgumentException> {
            Category.create(input, "2024-01-01T00:00:00Z")
        }
    }

    // ─── Property Tests ────────────────────────

    @Test
    fun `isDeleted returns true when deletedAt is set`() {
        val category = Category(
            id = CategoryId(1),
            name = "Test",
            registeredAt = "2024-01-01T00:00:00Z",
            deletedAt = "2024-06-01T12:00:00Z"
        )
        assertTrue(category.isDeleted)
    }

    @Test
    fun `isDeleted returns false when deletedAt is null`() {
        val category = Category(
            id = CategoryId(1),
            name = "Test",
            registeredAt = "2024-01-01T00:00:00Z",
            deletedAt = null
        )
        assertFalse(category.isDeleted)
    }

    // ─── Immutability Tests ────────────────────

    @Test
    fun `Category is immutable via copy`() {
        val original = Category(
            id = CategoryId(1),
            name = "Original",
            registeredAt = "2024-01-01T00:00:00Z",
            deletedAt = null
        )
        val modified = original.copy(name = "Modified")

        assertEquals("Original", original.name)
        assertEquals("Modified", modified.name)
    }

    @Test
    fun `Category equality is by value`() {
        val a = Category(
            id = CategoryId(1),
            name = "Test",
            registeredAt = "2024-01-01T00:00:00Z",
            deletedAt = null
        )
        val b = Category(
            id = CategoryId(1),
            name = "Test",
            registeredAt = "2024-01-01T00:00:00Z",
            deletedAt = null
        )

        assertEquals(a, b)
    }
}
