import { openDatabaseSync, type SQLiteDatabase } from 'expo-sqlite';

import { SqliteResumenCajaRepository } from '@/data/repositories/SqliteResumenCajaRepository';
import { SEED_SQL } from '@/data/database/schema.sql';

describe('SqliteResumenCajaRepository', () => {
  let db: SQLiteDatabase;
  let repo: SqliteResumenCajaRepository;

  beforeEach(() => {
    db = openDatabaseSync(':memory:');
    db.execSync(
      `CREATE TABLE IF NOT EXISTS resumen_caja (
        id INTEGER PRIMARY KEY CHECK (id = 1),
        total_ventas REAL DEFAULT 0.0,
        total_compras REAL DEFAULT 0.0,
        total_gastos REAL DEFAULT 0.0,
        saldo_actual REAL DEFAULT 0.0
      )`,
    );
    db.execSync(SEED_SQL);
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
        'UPDATE resumen_caja SET total_ventas = 1000, saldo_actual = 500 WHERE id = 1',
      );
      const r = repo.obtener();
      expect(r.totalVentas).toBe(1000);
      expect(r.saldoActual).toBe(500);
    });
  });
});
