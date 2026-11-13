import type { SQLiteDatabase } from 'expo-sqlite';

import type { ResumenCaja } from '@/domain/entities/ResumenCaja';
import { toResumenCaja, type ResumenCajaRow } from '@/data/mappers/rowMappers';
import { normalizeAccumulatorDelta } from '@/domain/factories/FinancialAccumulatorFactory';
import type { IResumenCajaRepository } from './IResumenCajaRepository';

const ACCUMULATOR_COLUMNS = 'total_sales, total_purchases, total_expenses, net_balance';

export class SqliteResumenCajaRepository implements IResumenCajaRepository {
  constructor(private readonly db: SQLiteDatabase) {}

  obtener(): ResumenCaja {
    const row = this.db.getFirstSync<ResumenCajaRow>(
      `SELECT ${ACCUMULATOR_COLUMNS} FROM financial_accumulators WHERE id = 1`,
    );
    if (!row) {
      return { totalVentas: 0, totalCompras: 0, totalGastos: 0, saldoActual: 0 };
    }
    return toResumenCaja(row);
  }

  aplicarDelta(input: {
    readonly salesDelta?: number;
    readonly purchasesDelta?: number;
    readonly expensesDelta?: number;
  }): ResumenCaja {
    const delta = normalizeAccumulatorDelta(input);
    this.db.withTransactionSync(() => {
      this.db.runSync(
        `UPDATE financial_accumulators
            SET total_sales = total_sales + ?,
                total_purchases = total_purchases + ?,
                total_expenses = total_expenses + ?,
                net_balance = net_balance + ?,
                updated_at = CURRENT_TIMESTAMP
          WHERE id = 1`,
        delta.salesDelta,
        delta.purchasesDelta,
        delta.expensesDelta,
        delta.netDelta,
      );
    });
    return this.obtener();
  }
}
