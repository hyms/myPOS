package com.tpvmovilexpress.data.repositories.impl

import com.tpvmovilexpress.data.database.TPVDatabase
import com.tpvmovilexpress.data.repositories.IStockMovementRepository
import com.tpvmovilexpress.domain.entities.ProductId
import com.tpvmovilexpress.domain.entities.StockMovement
import com.tpvmovilexpress.domain.entities.StockMovementId
import com.tpvmovilexpress.domain.entities.StockMovementType
import com.tpvmovilexpress.domain.entities.TransactionId
import com.tpvmovilexpress.domain.valueobjects.Stock
import kotlinx.datetime.Clock

/**
 * SQLDelight implementation of [IStockMovementRepository].
 * Stock movements are append-only audit records.
 */
public class SqlDelightStockMovementRepository(
    private val database: TPVDatabase
) : IStockMovementRepository {

    private val queries get() = database.stockMovementsQueries

    override fun create(
        productId: ProductId,
        type: StockMovementType,
        quantity: Stock,
        sourceTransactionId: Long?,
        note: String?
    ): StockMovement {
        require(!quantity.isZero) { "Stock movement quantity cannot be zero" }

        val nowEpochMillis = Clock.System.now().toEpochMilliseconds()

        queries.insert(
            productId = productId.value,
            type = type.code,
            quantity = quantity.amount,
            sourceTransactionId = sourceTransactionId,
            note = note,
            registeredAt = nowEpochMillis
        )

        val newId = queries.lastInsertId().executeAsOne()

        return StockMovement(
            id = StockMovementId(newId),
            productId = productId,
            type = type,
            quantity = quantity,
            sourceTransactionId = sourceTransactionId?.let { TransactionId(it) },
            note = note,
            registeredAt = epochMillisToIso(nowEpochMillis)
        )
    }

    override fun listByProductId(
        productId: ProductId,
        limit: Int,
        offset: Int
    ): List<StockMovement> =
        queries.selectByProductId(
            productId = productId.value,
            limit = limit.toLong(),
            offset = offset.toLong()
        ).executeAsList().map { it.toDomain() }

    override fun findById(id: StockMovementId): StockMovement? =
        queries.selectById(id.value).executeAsOneOrNull()?.toDomain()

    private fun epochMillisToIso(epochMillis: Long): String =
        kotlinx.datetime.Instant.fromEpochMilliseconds(epochMillis).toString()

    private fun com.tpvmovilexpress.data.database.Stock_movements.toDomain(): StockMovement =
        StockMovement(
            id = StockMovementId(this.id),
            productId = ProductId(this.product_id),
            type = StockMovementType.fromCode(this.type),
            quantity = Stock.of(this.quantity),
            sourceTransactionId = this.source_transaction_id?.let { TransactionId(it) },
            note = this.note,
            registeredAt = epochMillisToIso(this.registered_at)
        )
}
