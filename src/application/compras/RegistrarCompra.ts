import { getDatabase } from '@/data/database/client';
import { calcTotals } from '@/domain/rules/cartRules';
import type { CarritoItem } from '@/domain/entities/CarritoItem';
import type { TipoPago } from '@/domain/entities/Transaccion';
import type { CurrencyCode } from '@/domain/value-objects/Currency';
import { toCartKind, toCartLine, toCartLines } from '@/shared/utils/legacyCart';

export interface RegistrarCompraInput {
  readonly items: ReadonlyArray<CarritoItem>;
  readonly tipoPago: TipoPago;
  readonly detalle: string | null;
  readonly currency: CurrencyCode;
}

export interface RegistrarCompraResult {
  readonly transaccionId: number;
  readonly total: number;
}

export function registrarCompra(input: RegistrarCompraInput): RegistrarCompraResult {
  if (input.items.length === 0) {
    throw new Error('El carrito está vacío.');
  }

  const kind = toCartKind('COMPRA');
  const totals = calcTotals(toCartLines(input.items, kind, input.currency), input.currency);

  if (totals.total.amount <= 0) {
    throw new Error('El total de la compra debe ser mayor a 0.');
  }

  const db = getDatabase();
  let transaccionId = 0;
  db.withTransactionSync(() => {
    const trxResult = db.runSync(
      "INSERT INTO sales_transactions (type, total_amount, payment_method, detail) VALUES ('COMPRA', ?, ?, ?)",
      totals.total.amount,
      input.tipoPago,
      input.detalle,
    );
    transaccionId = trxResult.lastInsertRowId;

    for (const item of input.items) {
      const line = toCartLine(item, kind, input.currency);
      db.runSync(
        'INSERT INTO transaction_details (transaction_id, product_id, quantity, unit_price) VALUES (?, ?, ?, ?)',
        transaccionId,
        item.producto.id,
        item.cantidad,
        line.unitPrice.amount,
      );
      db.runSync(
        'UPDATE products SET current_stock = current_stock + ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?',
        item.cantidad,
        item.producto.id,
      );
    }

    db.runSync(
      'UPDATE financial_accumulators SET total_purchases = total_purchases + ?, net_balance = net_balance - ?, updated_at = CURRENT_TIMESTAMP WHERE id = 1',
      totals.total.amount,
      totals.total.amount,
    );
  });

  return { transaccionId, total: totals.total.amount };
}
