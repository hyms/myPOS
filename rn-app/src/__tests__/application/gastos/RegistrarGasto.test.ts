import { type SQLiteDatabase } from 'expo-sqlite';

import { registrarGasto } from '@/application/gastos/RegistrarGasto';
import { createMigratedDatabase } from '@/__tests__/support/database';

const mockGetDatabase = jest.fn();

jest.mock('@/data/database/client', () => ({
  getDatabase: () => mockGetDatabase(),
  resetDatabaseForTests: jest.fn(),
}));

function createFreshDb(): SQLiteDatabase {
  return createMigratedDatabase();
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
      total_expenses: number;
      net_balance: number;
    }>('SELECT total_expenses, net_balance FROM financial_accumulators WHERE id = 1');
    expect(row!.total_expenses).toBe(50.5);
    expect(row!.net_balance).toBe(-50.5);

    const trx = db.getFirstSync<{ type: string; detail: string }>(
      'SELECT type, detail FROM sales_transactions WHERE id = ?',
      result.transaccionId,
    );
    expect(trx!.type).toBe('GASTO');
    expect(trx!.detail).toBe('Luz');
  });

  it('acepta monto con decimales', () => {
    const result = registrarGasto({
      monto: 99.99,
      descripcion: 'Internet',
      tipoPago: 'TRANSFERENCIA',
    });
    expect(result.transaccionId).toBeGreaterThan(0);

    const trx = db.getFirstSync<{ total_amount: number; payment_method: string }>(
      'SELECT total_amount, payment_method FROM sales_transactions WHERE id = ?',
      result.transaccionId,
    );
    expect(trx!.total_amount).toBe(99.99);
    expect(trx!.payment_method).toBe('TRANSFERENCIA');
  });
});
