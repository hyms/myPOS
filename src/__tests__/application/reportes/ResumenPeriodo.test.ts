import { openDatabaseSync, type SQLiteDatabase } from 'expo-sqlite';

import {
  obtenerResumenCaja,
  obtenerResumenMes,
  obtenerResumenAnio,
} from '@/application/reportes/ResumenPeriodo';
import { SqliteProductoRepository } from '@/data/repositories/SqliteProductoRepository';
import { SqliteCategoriaRepository } from '@/data/repositories/SqliteCategoriaRepository';
import { SqliteTransaccionRepository } from '@/data/repositories/SqliteTransaccionRepository';
import { SqliteResumenCajaRepository } from '@/data/repositories/SqliteResumenCajaRepository';
import { SCHEMA_SQL, SEED_SQL } from '@/data/database/schema.sql';

const mockGetRepositories = jest.fn();

jest.mock('@/data/repositories/container', () => ({
  getRepositories: () => mockGetRepositories(),
  resetRepositoriesForTests: jest.fn(),
}));

interface TestDb {
  db: SQLiteDatabase;
  productos: SqliteProductoRepository;
  categorias: SqliteCategoriaRepository;
  transacciones: SqliteTransaccionRepository;
  resumenCaja: SqliteResumenCajaRepository;
}

function createTestDb(): TestDb {
  const db = openDatabaseSync(':memory:');
  db.execSync(SCHEMA_SQL);
  db.execSync(SEED_SQL);
  return {
    db,
    productos: new SqliteProductoRepository(db),
    categorias: new SqliteCategoriaRepository(db),
    transacciones: new SqliteTransaccionRepository(db),
    resumenCaja: new SqliteResumenCajaRepository(db),
  };
}

describe('ResumenPeriodo', () => {
  let t: TestDb;

  beforeEach(() => {
    t = createTestDb();
    mockGetRepositories.mockReturnValue({
      productos: t.productos,
      categorias: t.categorias,
      transacciones: t.transacciones,
      resumenCaja: t.resumenCaja,
    });
  });

  describe('obtenerResumenCaja', () => {
    it('retorna resumen con valores iniciales', () => {
      const r = obtenerResumenCaja();
      expect(r.totalVentas).toBe(0);
      expect(r.totalCompras).toBe(0);
      expect(r.totalGastos).toBe(0);
      expect(r.saldoActual).toBe(0);
    });

    it('retorna valores actualizados', () => {
      t.db.runSync(
        'UPDATE resumen_caja SET total_ventas = 1000, saldo_actual = 500 WHERE id = 1',
      );
      const r = obtenerResumenCaja();
      expect(r.totalVentas).toBe(1000);
      expect(r.saldoActual).toBe(500);
    });
  });

  describe('obtenerResumenAnio', () => {
    it('retorna neto calculado correctamente', () => {
      t.db.runSync(
        "INSERT INTO transacciones (tipo, monto_total, tipo_pago) VALUES ('VENTA', ?, ?)",
        1000,
        'EFECTIVO',
      );
      t.db.runSync(
        "INSERT INTO transacciones (tipo, monto_total, tipo_pago) VALUES ('COMPRA', ?, ?)",
        300,
        'EFECTIVO',
      );
      t.db.runSync(
        "INSERT INTO transacciones (tipo, monto_total, tipo_pago) VALUES ('GASTO', ?, ?)",
        100,
        'EFECTIVO',
      );

      const r = obtenerResumenAnio();
      expect(r.totalVentas).toBe(1000);
      expect(r.totalCompras).toBe(300);
      expect(r.totalGastos).toBe(100);
      expect(r.neto).toBe(600); // 1000 - 300 - 100
    });
  });
});
