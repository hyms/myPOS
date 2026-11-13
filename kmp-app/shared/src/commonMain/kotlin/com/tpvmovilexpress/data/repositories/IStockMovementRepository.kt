package com.tpvmovilexpress.data.repositories

import com.tpvmovilexpress.domain.entities.ProductId
import com.tpvmovilexpress.domain.entities.StockMovement
import com.tpvmovilexpress.domain.entities.StockMovementId
import com.tpvmovilexpress.domain.entities.StockMovementType
import com.tpvmovilexpress.domain.valueobjects.Stock

/**
 * Repository port for [StockMovement] records.
 * Stock movements are append-only audit records (no update or delete).
 */
public interface IStockMovementRepository {

    /**
     * Records a new stock movement.
     * @return The created movement with its assigned ID
     */
    public fun create(
        productId: ProductId,
        type: StockMovementType,
        quantity: Stock,
        sourceTransactionId: Long? = null,
        note: String? = null
    ): StockMovement

    /**
     * Lists all stock movements for a given product, newest first.
     */
    public fun listByProductId(productId: ProductId, limit: Int = 50, offset: Int = 0): List<StockMovement>

    /**
     * Finds a stock movement by ID.
     */
    public fun findById(id: StockMovementId): StockMovement?
}
