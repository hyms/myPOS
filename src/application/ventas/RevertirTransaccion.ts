import { getRepositories } from '@/data/repositories/container';
import { getDatabase } from '@/data/database/client';
import { StockError } from '@/domain/rules/stockRules';

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
      if (producto.stockActual - det.cantidad < 0) {
        throw new StockError(
          `No se puede revertir la compra: "${producto.nombre}" quedaría con stock negativo.`,
          producto.id,
          producto.nombre,
        );
      }
    }
  }

  const db = getDatabase();
  db.withTransactionSync(() => {
    if (trx.tipo === 'VENTA') {
      for (const det of detalles) {
        db.runSync(
          'UPDATE productos SET stock_actual = stock_actual + ?, fecha_update = CURRENT_TIMESTAMP WHERE id = ?',
          det.cantidad,
          det.productoId,
        );
      }
      db.runSync(
        'UPDATE resumen_caja SET total_ventas = total_ventas - ?, saldo_actual = saldo_actual - ? WHERE id = 1',
        trx.montoTotal,
        trx.montoTotal,
      );
    } else if (trx.tipo === 'COMPRA') {
      for (const det of detalles) {
        db.runSync(
          'UPDATE productos SET stock_actual = stock_actual - ? WHERE id = ?',
          det.cantidad,
          det.productoId,
        );
      }
      db.runSync(
        'UPDATE resumen_caja SET total_compras = total_compras - ?, saldo_actual = saldo_actual + ? WHERE id = 1',
        trx.montoTotal,
        trx.montoTotal,
      );
    } else {
      db.runSync(
        'UPDATE resumen_caja SET total_gastos = total_gastos - ?, saldo_actual = saldo_actual + ? WHERE id = 1',
        trx.montoTotal,
        trx.montoTotal,
      );
    }
    db.runSync(
      'UPDATE transacciones SET fecha_delete = CURRENT_TIMESTAMP WHERE id = ?',
      id,
    );
  });

  return { transaccionId: id, tipo: trx.tipo };
}
