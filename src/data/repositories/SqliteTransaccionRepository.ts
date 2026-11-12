import type { SQLiteDatabase } from 'expo-sqlite';

import type { DetalleTransaccion } from '@/domain/entities/DetalleTransaccion';
import type { TipoTransaccion, Transaccion } from '@/domain/entities/Transaccion';
import {
  toDetalleTransaccion,
  toTransaccion,
  type DetalleTransaccionRow,
  type TransaccionRow,
} from '@/data/mappers/rowMappers';
import type { ITransaccionRepository } from './ITransaccionRepository';

const TRANSACTION_COLUMNS =
  'id, type, total_amount, payment_method, detail, registered_at, deleted_at';

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
      'SELECT id, transaction_id, product_id, quantity, unit_price FROM transaction_details WHERE transaction_id = ?',
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
}
