import { getDatabase } from '@/data/database/client';
import { calcTotals } from '@/domain/rules/cartRules';
import { validateStockDisponible } from '@/domain/rules/stockRules';
import type { CarritoItem } from '@/domain/entities/CarritoItem';
import type { TipoPago } from '@/domain/entities/Transaccion';

export interface RegistrarVentaInput {
  readonly items: ReadonlyArray<CarritoItem>;
  readonly tipoPago: TipoPago;
  readonly detalle: string | null;
}

export interface RegistrarVentaResult {
  readonly transaccionId: number;
  readonly total: number;
  readonly stockAlerts: ReadonlyArray<{ id: number; nombre: string; stockActual: number; stockMinimo: number }>;
}

export function registrarVenta(input: RegistrarVentaInput): RegistrarVentaResult {
  if (input.items.length === 0) {
    throw new Error('El carrito está vacío.');
  }
  const totals = calcTotals(input.items, 'VENTA');
  if (totals.total <= 0) {
    throw new Error('El total de la venta debe ser mayor a 0.');
  }

  const db = getDatabase();
  for (const item of input.items) {
    validateStockDisponible(
      { id: item.producto.id, nombre: item.producto.nombre, stockActual: item.producto.stockActual },
      item.cantidad,
    );
  }

  let transaccionId = 0;
  db.withTransactionSync(() => {
    const trxResult = db.runSync(
      "INSERT INTO transacciones (tipo, monto_total, tipo_pago, detalle) VALUES ('VENTA', ?, ?, ?)",
      totals.total,
      input.tipoPago,
      input.detalle,
    );
    transaccionId = trxResult.lastInsertRowId;

    for (const item of input.items) {
      const precioUnitarioReal =
        item.cantidad > 0
          ? (item.producto.precioVenta * item.cantidad - item.descuento) / item.cantidad
          : item.producto.precioVenta;
      db.runSync(
        'INSERT INTO detalle_transacciones (transaccion_id, producto_id, cantidad, precio_unitario) VALUES (?, ?, ?, ?)',
        transaccionId,
        item.producto.id,
        item.cantidad,
        precioUnitarioReal,
      );
      db.runSync(
        'UPDATE productos SET stock_actual = stock_actual - ?, fecha_update = CURRENT_TIMESTAMP WHERE id = ?',
        item.cantidad,
        item.producto.id,
      );
    }

    db.runSync(
      'UPDATE resumen_caja SET total_ventas = total_ventas + ?, saldo_actual = saldo_actual + ? WHERE id = 1',
      totals.total,
      totals.total,
    );
  });

  const stockAlerts = collectStockAlerts(input.items, -1);
  return { transaccionId, total: totals.total, stockAlerts };
}

function collectStockAlerts(
  items: ReadonlyArray<CarritoItem>,
  delta: 1 | -1,
): ReadonlyArray<{ id: number; nombre: string; stockActual: number; stockMinimo: number }> {
  const db = getDatabase();
  const ids = items.map((item) => item.producto.id);
  if (ids.length === 0) return [];
  const placeholders = ids.map(() => '?').join(',');
  const rows = db.getAllSync<{ id: number; nombre: string; stock_actual: number; stock_minimo: number }>(
    `SELECT id, nombre, stock_actual, stock_minimo FROM productos WHERE id IN (${placeholders})`,
    ...ids,
  );
  return rows
    .map((row) => {
      const item = items.find((i) => i.producto.id === row.id);
      const stockActual = row.stock_actual;
      if (stockActual < row.stock_minimo) {
        return {
          id: row.id,
          nombre: row.nombre,
          stockActual,
          stockMinimo: row.stock_minimo,
        };
      }
      return null;
    })
    .filter((v): v is { id: number; nombre: string; stockActual: number; stockMinimo: number } => v !== null);
}
