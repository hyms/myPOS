import { getDatabase } from '@/data/database/client';
import { calcTotals } from '@/domain/rules/cartRules';
import type { CarritoItem } from '@/domain/entities/CarritoItem';
import type { TipoPago } from '@/domain/entities/Transaccion';

export interface RegistrarCompraInput {
  readonly items: ReadonlyArray<CarritoItem>;
  readonly tipoPago: TipoPago;
  readonly detalle: string | null;
}

export interface RegistrarCompraResult {
  readonly transaccionId: number;
  readonly total: number;
}

export function registrarCompra(input: RegistrarCompraInput): RegistrarCompraResult {
  if (input.items.length === 0) {
    throw new Error('El carrito está vacío.');
  }
  const totals = calcTotals(input.items, 'COMPRA');
  if (totals.total <= 0) {
    throw new Error('El total de la compra debe ser mayor a 0.');
  }

  const db = getDatabase();
  let transaccionId = 0;
  db.withTransactionSync(() => {
    const trxResult = db.runSync(
      "INSERT INTO transacciones (tipo, monto_total, tipo_pago, detalle) VALUES ('COMPRA', ?, ?, ?)",
      totals.total,
      input.tipoPago,
      input.detalle,
    );
    transaccionId = trxResult.lastInsertRowId;

    for (const item of input.items) {
      const unitPrice = item.producto.precioCompra;
      db.runSync(
        'INSERT INTO detalle_transacciones (transaccion_id, producto_id, cantidad, precio_unitario) VALUES (?, ?, ?, ?)',
        transaccionId,
        item.producto.id,
        item.cantidad,
        unitPrice,
      );
      db.runSync(
        'UPDATE productos SET stock_actual = stock_actual + ?, fecha_update = CURRENT_TIMESTAMP WHERE id = ?',
        item.cantidad,
        item.producto.id,
      );
    }

    db.runSync(
      'UPDATE resumen_caja SET total_compras = total_compras + ?, saldo_actual = saldo_actual - ? WHERE id = 1',
      totals.total,
      totals.total,
    );
  });

  return { transaccionId, total: totals.total };
}
