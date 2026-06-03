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

export class SqliteTransaccionRepository implements ITransaccionRepository {
  constructor(private readonly db: SQLiteDatabase) {}

  findById(id: number): Transaccion | null {
    const row = this.db.getFirstSync<TransaccionRow>(
      'SELECT id, tipo, monto_total, tipo_pago, detalle, fecha_registro, fecha_delete FROM transacciones WHERE id = ?',
      id,
    );
    return row ? toTransaccion(row) : null;
  }

  findDetalles(id: number): ReadonlyArray<DetalleTransaccion> {
    const rows = this.db.getAllSync<DetalleTransaccionRow>(
      'SELECT id, transaccion_id, producto_id, cantidad, precio_unitario FROM detalle_transacciones WHERE transaccion_id = ?',
      id,
    );
    return rows.map(toDetalleTransaccion);
  }

  listar({ tipo, limit, offset }: { tipo?: TipoTransaccion; limit: number; offset: number }): ReadonlyArray<Transaccion> {
    const sql = tipo
      ? `SELECT id, tipo, monto_total, tipo_pago, detalle, fecha_registro, fecha_delete
           FROM transacciones
          WHERE fecha_delete IS NULL AND tipo = ?
          ORDER BY fecha_registro DESC
          LIMIT ? OFFSET ?`
      : `SELECT id, tipo, monto_total, tipo_pago, detalle, fecha_registro, fecha_delete
           FROM transacciones
          WHERE fecha_delete IS NULL
          ORDER BY fecha_registro DESC
          LIMIT ? OFFSET ?`;
    const rows = tipo
      ? this.db.getAllSync<TransaccionRow>(sql, tipo, limit, offset)
      : this.db.getAllSync<TransaccionRow>(sql, limit, offset);
    return rows.map(toTransaccion);
  }

  listarPorMesAnio({ periodo }: { periodo: 'MONTH' | 'YEAR' }): ReadonlyArray<{ tipo: TipoTransaccion; total: number }> {
    const monthFilter = periodo === 'MONTH'
      ? `AND strftime('%m', fecha_registro) = strftime('%m', 'now', 'localtime')`
      : '';
    const rows = this.db.getAllSync<{ tipo: string; total: number }>(
      `SELECT tipo, SUM(monto_total) AS total
         FROM transacciones
        WHERE fecha_delete IS NULL
          AND strftime('%Y', fecha_registro) = strftime('%Y', 'now', 'localtime')
          ${monthFilter}
        GROUP BY tipo`,
    );
    return rows.map((row) => ({ tipo: row.tipo as TipoTransaccion, total: row.total ?? 0 }));
  }

  gastosDelMes(limit: number, offset: number): ReadonlyArray<Transaccion> {
    const rows = this.db.getAllSync<TransaccionRow>(
      `SELECT id, tipo, monto_total, tipo_pago, detalle, fecha_registro, fecha_delete
         FROM transacciones
        WHERE tipo = 'GASTO'
          AND fecha_delete IS NULL
          AND strftime('%Y-%m', fecha_registro) = strftime('%Y-%m', 'now', 'localtime')
        ORDER BY fecha_registro DESC
        LIMIT ? OFFSET ?`,
      limit,
      offset,
    );
    return rows.map(toTransaccion);
  }
}
