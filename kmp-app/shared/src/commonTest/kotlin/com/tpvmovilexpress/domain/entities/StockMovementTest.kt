package com.tpvmovilexpress.domain.entities

import com.tpvmovilexpress.domain.valueobjects.Stock
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class StockMovementTest {

    // ─── Factory Tests ─────────────────────────

    @Test
    fun `create builds valid IN movement`() {
        val movement = StockMovement.create(
            productId = ProductId(1),
            type = StockMovementType.IN,
            quantity = Stock.of(50.0),
            sourceTransactionId = TransactionId(100),
            note = "Purchase received",
            now = "2024-06-01T12:00:00Z"
        )

        assertEquals(ProductId(1), movement.productId)
        assertEquals(StockMovementType.IN, movement.type)
        assertEquals(50.0, movement.quantity.amount)
        assertEquals(TransactionId(100), movement.sourceTransactionId)
        assertEquals("Purchase received", movement.note)
        assertEquals("2024-06-01T12:00:00Z", movement.registeredAt)
    }

    @Test
    fun `create builds valid OUT movement`() {
        val movement = StockMovement.create(
            productId = ProductId(2),
            type = StockMovementType.OUT,
            quantity = Stock.of(10.5),
            now = "2024-06-01T12:00:00Z"
        )

        assertEquals(StockMovementType.OUT, movement.type)
        assertEquals(10.5, movement.quantity.amount)
        assertNull(movement.sourceTransactionId)
        assertNull(movement.note)
    }

    @Test
    fun `create builds valid ADJUSTMENT movement`() {
        val movement = StockMovement.create(
            productId = ProductId(3),
            type = StockMovementType.ADJUSTMENT,
            quantity = Stock.of(5.0),
            note = "Inventory audit correction",
            now = "2024-06-01T12:00:00Z"
        )

        assertEquals(StockMovementType.ADJUSTMENT, movement.type)
        assertEquals("Inventory audit correction", movement.note)
    }

    @Test
    fun `create throws for zero quantity`() {
        assertFailsWith<IllegalArgumentException> {
            StockMovement.create(
                productId = ProductId(1),
                type = StockMovementType.IN,
                quantity = Stock.zero(),
                now = "2024-01-01T00:00:00Z"
            )
        }
    }

    // ─── StockMovementType Tests ───────────────

    @Test
    fun `StockMovementType fromCode resolves IN`() {
        assertEquals(StockMovementType.IN, StockMovementType.fromCode("IN"))
        assertEquals(StockMovementType.IN, StockMovementType.fromCode("in"))
    }

    @Test
    fun `StockMovementType fromCode resolves OUT`() {
        assertEquals(StockMovementType.OUT, StockMovementType.fromCode("OUT"))
        assertEquals(StockMovementType.OUT, StockMovementType.fromCode("out"))
    }

    @Test
    fun `StockMovementType fromCode resolves ADJUSTMENT`() {
        assertEquals(StockMovementType.ADJUSTMENT, StockMovementType.fromCode("ADJUSTMENT"))
        assertEquals(StockMovementType.ADJUSTMENT, StockMovementType.fromCode("adjustment"))
    }

    @Test
    fun `StockMovementType fromCode throws for invalid code`() {
        assertFailsWith<IllegalArgumentException> {
            StockMovementType.fromCode("INVALID")
        }
    }

    // ─── Immutability Tests ────────────────────

    @Test
    fun `StockMovement is immutable via copy`() {
        val original = StockMovement.create(
            productId = ProductId(1),
            type = StockMovementType.IN,
            quantity = Stock.of(10.0),
            now = "2024-01-01T00:00:00Z"
        )
        val modified = original.copy(note = "Updated note")

        assertNull(original.note)
        assertEquals("Updated note", modified.note)
    }
}
