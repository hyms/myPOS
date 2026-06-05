import { openDatabaseSync, type SQLiteDatabase } from 'expo-sqlite';

import { registrarGasto } from '@/application/gastos/RegistrarGasto';
import { SEED_SQL } from '@/data/database/schema.sql';

const mockGetDatabase = jest.fn();

jest.mock('@/data/database/client', () => ({
  getDatabase: () => mockGetDatabase(),
  resetDatabaseForTests: jest.fn(),
}));

function createFreshDb(): SQLiteDatabase {
  const db = openDatabaseSync(':memory:');
  db.execSync(`
    CREATE TABLE IF NOT EXISTS transacciones (
      id INTEGER PRIMARY KEY AUTOINCREMENT,
      tipo TEXT NOT NULL,
      monto_total REAL NOT NULL,
      tipo_pago TEXT NOT NULL,
      detalle TEXT,
      fecha_registro TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
      fecha_delete TIMESTAMP DEFAULT NULL
    );
    CREATE TABLE IF NOT EXISTS resumen_caja (
      id INTEGER PRIMARY KEY CHECK (id = 1),
      total_ventas REAL DEFAULT 0.0,
      total_compras REAL DEFAULT 0.0,
      total_gastos REAL DEFAULT 0.0,
      saldo_actual REAL DEFAULT 0.0
    );
  `);
  db.execSync(SEED_SQL);
  return db;
}

describe('registrarGasto', () => {
  let db: SQLiteDatabase;

  beforeEach(() => {
    db = createFreshDb();
    mockGetDatabase.mockReturnValue(db);
  });

  it('lanza error si monto <= 0', () => {
    expect(() =>
      registrarGasto({ monto: 0, descripcion: 'Test', tipoPago: 'EFECTIVO' }),
    ).toThrow('mayor a 0');
    expect(() =>
      registrarGasto({ monto: -1, descripcion: 'Test', tipoPago: 'EFECTIVO' }),
    ).toThrow('mayor a 0');
  });

  it('lanza error si descripción vacía', () => {
    expect(() =>
      registrarGasto({ monto: 100, descripcion: '', tipoPago: 'EFECTIVO' }),
    ).toThrow('obligatoria');
    expect(() =>
      registrarGasto({ monto: 100, descripcion: '   ', tipoPago: 'EFECTIVO' }),
    ).toThrow('obligatoria');
  });

  it('registra un gasto y actualiza caja', () => {
    const result = registrarGasto({
      monto: 50.5,
      descripcion: 'Luz',
      tipoPago: 'EFECTIVO',
    });
    expect(result.transaccionId).toBeGreaterThan(0);

    const row = db.getFirstSync<{
      total_gastos: number;
      saldo_actual: number;
    }>('SELECT total_gastos, saldo_actual FROM resumen_caja WHERE id = 1');
    expect(row!.total_gastos).toBe(50.5);
    expect(row!.saldo_actual).toBe(-50.5);

    const trx = db.getFirstSync<{ tipo: string; detalle: string }>(
      'SELECT tipo, detalle FROM transacciones WHERE id = ?',
      result.transaccionId,
    );
    expect(trx!.tipo).toBe('GASTO');
    expect(trx!.detalle).toBe('Luz');
  });

  it('acepta monto con decimales', () => {
    const result = registrarGasto({
      monto: 99.99,
      descripcion: 'Internet',
      tipoPago: 'TRANSFERENCIA',
    });
    expect(result.transaccionId).toBeGreaterThan(0);

    const trx = db.getFirstSync<{ monto_total: number; tipo_pago: string }>(
      'SELECT monto_total, tipo_pago FROM transacciones WHERE id = ?',
      result.transaccionId,
    );
    expect(trx!.monto_total).toBe(99.99);
    expect(trx!.tipo_pago).toBe('TRANSFERENCIA');
  });
});
