import type { SQLiteDatabase } from 'expo-sqlite';

import type { DetalleTransaccion } from '@/domain/entities/DetalleTransaccion';
import type { TipoTransaccion, Transaccion } from '@/domain/entities/Transaccion';
import {
  toDetalleTransaccion,
  toTransaccion,
  type DetalleTransaccionRow,
  type TransaccionRow,
} from '@/data/mappers/rowMappers';
import type {
  CreateDetalleInput,
  CreateTransaccionInput,
  ITransaccionRepository,
} from './ITransaccionRepository';

const TRANSACTION_COLUMNS =
  'id, type, total_amount, payment_method, detail, registered_at, deleted_at';
const DETAIL_COLUMNS = 'id, transaction_id, product_id, quantity, unit_price';

export class SqliteTransaccionRepository implements ITransaccionRepository {
  constructor(private readonly db: SQLiteDatabase) {}

  findById(id: number): Transaccion | null {
    const row = this.db.getFirstSync<TransaccionRow>(
      `SELECT ${TRANSACTION_COLUMNS} FROM sales_transactions WHERE id = ?`,
      id,
    );
    return row ? toTransaccion(row) : null;
  }

  findDetalles(id: number): ReadonlyArray<DetalleTransaccion> {
    const rows = this.db.getAllSync<DetalleTransaccionRow>(
      `SELECT ${DETAIL_COLUMNS} FROM transaction_details WHERE transaction_id = ?`,
      id,
    );
    return rows.map(toDetalleTransaccion);
  }

  listar({ tipo, limit, offset }: { tipo?: TipoTransaccion; limit: number; offset: number }): ReadonlyArray<Transaccion> {
    const sql = tipo
      ? `SELECT ${TRANSACTION_COLUMNS}
           FROM sales_transactions
          WHERE deleted_at IS NULL AND type = ?
          ORDER BY registered_at DESC
          LIMIT ? OFFSET ?`
      : `SELECT ${TRANSACTION_COLUMNS}
           FROM sales_transactions
          WHERE deleted_at IS NULL
          ORDER BY registered_at DESC
          LIMIT ? OFFSET ?`;
    const rows = tipo
      ? this.db.getAllSync<TransaccionRow>(sql, tipo, limit, offset)
      : this.db.getAllSync<TransaccionRow>(sql, limit, offset);
    return rows.map(toTransaccion);
  }

  listarPorMesAnio({ periodo }: { periodo: 'MONTH' | 'YEAR' }): ReadonlyArray<{ tipo: TipoTransaccion; total: number }> {
    const monthFilter = periodo === 'MONTH'
      ? `AND strftime('%m', registered_at) = strftime('%m', 'now', 'localtime')`
      : '';
    const rows = this.db.getAllSync<{ tipo: string; total: number }>(
      `SELECT type AS tipo, SUM(total_amount) AS total
         FROM sales_transactions
        WHERE deleted_at IS NULL
          AND strftime('%Y', registered_at) = strftime('%Y', 'now', 'localtime')
          ${monthFilter}
        GROUP BY type`,
    );
    return rows.map((row) => ({ tipo: row.tipo as TipoTransaccion, total: row.total ?? 0 }));
  }

  gastosDelMes(limit: number, offset: number): ReadonlyArray<Transaccion> {
    const rows = this.db.getAllSync<TransaccionRow>(
      `SELECT ${TRANSACTION_COLUMNS}
         FROM sales_transactions
        WHERE type = 'GASTO'
          AND deleted_at IS NULL
          AND strftime('%Y-%m', registered_at) = strftime('%Y-%m', 'now', 'localtime')
        ORDER BY registered_at DESC
        LIMIT ? OFFSET ?`,
      limit,
      offset,
    );
    return rows.map(toTransaccion);
  }

  crearTransaccion(input: CreateTransaccionInput): Transaccion {
    let createdId = 0;
    this.db.withTransactionSync(() => {
      const result = this.db.runSync(
        `INSERT INTO sales_transactions (type, total_amount, payment_method, detail)
         VALUES (?, ?, ?, ?)`,
        input.tipo,
        input.montoTotal,
        input.tipoPago,
        input.detalle,
      );
      createdId = Number(result.lastInsertRowId);
    });
    const created = this.findById(createdId);
    if (!created) {
      throw new Error('No se pudo crear la transacción.');
    }
    return created;
  }

  crearDetalle(transaccionId: number, input: CreateDetalleInput): DetalleTransaccion {
    let createdId = 0;
    this.db.withTransactionSync(() => {
      const result = this.db.runSync(
        `INSERT INTO transaction_details (transaction_id, product_id, quantity, unit_price)
         VALUES (?, ?, ?, ?)`,
        transaccionId,
        input.productoId,
        input.cantidad,
        input.precioUnitario,
      );
      createdId = Number(result.lastInsertRowId);
    });
    const row = this.db.getFirstSync<DetalleTransaccionRow>(
      `SELECT ${DETAIL_COLUMNS} FROM transaction_details WHERE id = ?`,
      createdId,
    );
    if (!row) {
      throw new Error('No se pudo crear el detalle de la transacción.');
    }
    return toDetalleTransaccion(row);
  }
}
