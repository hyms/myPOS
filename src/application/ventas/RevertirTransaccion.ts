import { getRepositories } from '@/data/repositories/container';
import { getDatabase } from '@/data/database/client';
import { assertStockAvailable } from '@/domain/rules/stockRules';
import { Stock } from '@/domain/value-objects/Stock';

export interface RevertirTransaccionResult {
  readonly transaccionId: number;
  readonly tipo: 'VENTA' | 'COMPRA' | 'GASTO';
}

export function revertirTransaccion(id: number): RevertirTransaccionResult {
  const repos = getRepositories();
  const trx = repos.transacciones.findById(id);
  if (!trx) {
    throw new Error(`La transacción #${id} no existe.`);
  }
  if (trx.fechaDelete !== null) {
    throw new Error(`La transacción #${id} ya fue revertida.`);
  }
  const detalles = repos.transacciones.findDetalles(id);

  if (trx.tipo === 'COMPRA') {
    for (const det of detalles) {
      const producto = repos.productos.findById(det.productoId);
      if (!producto) continue;
      if (det.cantidad > 0) {
        assertStockAvailable(
          {
            id: producto.id,
            name: producto.nombre,
            currentStock: new Stock(Math.max(0, producto.stockActual)),
          },
          new Stock(det.cantidad),
        );
      }
    }
  }

  const db = getDatabase();
  db.withTransactionSync(() => {
    if (trx.tipo === 'VENTA') {
      for (const det of detalles) {
        db.runSync(
          'UPDATE products SET current_stock = current_stock + ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?',
          det.cantidad,
          det.productoId,
        );
      }
      db.runSync(
        'UPDATE financial_accumulators SET total_sales = total_sales - ?, net_balance = net_balance - ?, updated_at = CURRENT_TIMESTAMP WHERE id = 1',
        trx.montoTotal,
        trx.montoTotal,
      );
    } else if (trx.tipo === 'COMPRA') {
      for (const det of detalles) {
        db.runSync(
          'UPDATE products SET current_stock = current_stock - ? WHERE id = ?',
          det.cantidad,
          det.productoId,
        );
      }
      db.runSync(
        'UPDATE financial_accumulators SET total_purchases = total_purchases - ?, net_balance = net_balance + ?, updated_at = CURRENT_TIMESTAMP WHERE id = 1',
        trx.montoTotal,
        trx.montoTotal,
      );
    } else {
      db.runSync(
        'UPDATE financial_accumulators SET total_expenses = total_expenses - ?, net_balance = net_balance + ?, updated_at = CURRENT_TIMESTAMP WHERE id = 1',
        trx.montoTotal,
        trx.montoTotal,
      );
    }
    db.runSync(
      'UPDATE sales_transactions SET deleted_at = CURRENT_TIMESTAMP WHERE id = ?',
      id,
    );
  });

  return { transaccionId: id, tipo: trx.tipo };
}
