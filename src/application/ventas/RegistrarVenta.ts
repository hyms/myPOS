import { getDatabase } from '@/data/database/client';
import { calcTotals, lineSubtotal } from '@/domain/rules/cartRules';
import { assertStockAvailable } from '@/domain/rules/stockRules';
import type { CarritoItem } from '@/domain/entities/CarritoItem';
import type { TipoPago } from '@/domain/entities/Transaccion';
import type { CurrencyCode } from '@/domain/value-objects/Currency';
import { Stock } from '@/domain/value-objects/Stock';
import { toCartKind, toCartLine, toCartLines } from '@/shared/utils/legacyCart';

export interface RegistrarVentaInput {
  readonly items: ReadonlyArray<CarritoItem>;
  readonly tipoPago: TipoPago;
  readonly detalle: string | null;
  readonly currency: CurrencyCode;
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

  const kind = toCartKind('VENTA');
  const totals = calcTotals(toCartLines(input.items, kind, input.currency), input.currency);

  if (totals.total.amount <= 0) {
    throw new Error('El total de la venta debe ser mayor a 0.');
  }

  for (const item of input.items) {
    assertStockAvailable(
      {
        id: item.producto.id,
        name: item.producto.nombre,
        currentStock: new Stock(Math.max(0, item.producto.stockActual)),
      },
      new Stock(Math.max(0, item.cantidad)),
    );
  }

  const db = getDatabase();
  let transaccionId = 0;
  db.withTransactionSync(() => {
    const trxResult = db.runSync(
      "INSERT INTO sales_transactions (type, total_amount, payment_method, detail) VALUES ('VENTA', ?, ?, ?)",
      totals.total.amount,
      input.tipoPago,
      input.detalle,
    );
    transaccionId = trxResult.lastInsertRowId;

    for (const item of input.items) {
      const line = toCartLine(item, kind, input.currency);
      const quantity = line.quantity.value;
      const net = lineSubtotal(line).subtract(line.discount);
      const unitPrice = quantity > 0 ? net.divide(quantity).amount : line.unitPrice.amount;
      db.runSync(
        'INSERT INTO transaction_details (transaction_id, product_id, quantity, unit_price) VALUES (?, ?, ?, ?)',
        transaccionId,
        item.producto.id,
        item.cantidad,
        unitPrice,
      );
      db.runSync(
        'UPDATE products SET current_stock = current_stock - ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?',
        item.cantidad,
        item.producto.id,
      );
    }

    db.runSync(
      'UPDATE financial_accumulators SET total_sales = total_sales + ?, net_balance = net_balance + ?, updated_at = CURRENT_TIMESTAMP WHERE id = 1',
      totals.total.amount,
      totals.total.amount,
    );
  });

  const stockAlerts = collectStockAlerts(input.items, -1);
  return { transaccionId, total: totals.total.amount, stockAlerts };
}

function collectStockAlerts(
  items: ReadonlyArray<CarritoItem>,
  delta: 1 | -1,
): ReadonlyArray<{ id: number; nombre: string; stockActual: number; stockMinimo: number }> {
  const db = getDatabase();
  const ids = items.map((item) => item.producto.id);
  if (ids.length === 0) return [];
  const placeholders = ids.map(() => '?').join(',');
  const rows = db.getAllSync<{ id: number; name: string; current_stock: number; min_stock: number }>(
    `SELECT id, name, current_stock, min_stock FROM products WHERE id IN (${placeholders})`,
    ...ids,
  );
  return rows
    .map((row) => {
      const stockActual = row.current_stock;
      if (stockActual < row.min_stock) {
        return {
          id: row.id,
          nombre: row.name,
          stockActual,
          stockMinimo: row.min_stock,
        };
      }
      return null;
    })
    .filter((v): v is { id: number; nombre: string; stockActual: number; stockMinimo: number } => v !== null);
}
