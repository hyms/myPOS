import { type SQLiteDatabase } from 'expo-sqlite';

import { SqliteResumenCajaRepository } from '@/data/repositories/SqliteResumenCajaRepository';
import { createMigratedDatabase } from '@/__tests__/support/database';

describe('SqliteResumenCajaRepository', () => {
  let db: SQLiteDatabase;
  let repo: SqliteResumenCajaRepository;

  beforeEach(() => {
    db = createMigratedDatabase();
    repo = new SqliteResumenCajaRepository(db);
  });

  describe('obtener', () => {
    it('retorna resumen con valores iniciales', () => {
      const r = repo.obtener();
      expect(r.totalVentas).toBe(0);
      expect(r.totalCompras).toBe(0);
      expect(r.totalGastos).toBe(0);
      expect(r.saldoActual).toBe(0);
    });

    it('retorna valores actualizados', () => {
      db.runSync(
        'UPDATE financial_accumulators SET total_sales = 1000, net_balance = 500 WHERE id = 1',
      );
      const r = repo.obtener();
      expect(r.totalVentas).toBe(1000);
      expect(r.saldoActual).toBe(500);
    });
  });
});
