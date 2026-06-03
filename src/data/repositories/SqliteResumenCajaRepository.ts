import type { SQLiteDatabase } from 'expo-sqlite';

import type { ResumenCaja } from '@/domain/entities/ResumenCaja';
import { toResumenCaja, type ResumenCajaRow } from '@/data/mappers/rowMappers';
import type { IResumenCajaRepository } from './IResumenCajaRepository';

export class SqliteResumenCajaRepository implements IResumenCajaRepository {
  constructor(private readonly db: SQLiteDatabase) {}

  obtener(): ResumenCaja {
    const row = this.db.getFirstSync<ResumenCajaRow>(
      'SELECT total_ventas, total_compras, total_gastos, saldo_actual FROM resumen_caja WHERE id = 1',
    );
    if (!row) {
      return { totalVentas: 0, totalCompras: 0, totalGastos: 0, saldoActual: 0 };
    }
    return toResumenCaja(row);
  }
}
