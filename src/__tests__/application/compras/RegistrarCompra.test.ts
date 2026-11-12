import { type SQLiteDatabase } from 'expo-sqlite';

import { registrarCompra } from '@/application/compras/RegistrarCompra';
import { createMigratedDatabase } from '@/__tests__/support/database';

const mockGetDatabase = jest.fn();

jest.mock('@/data/database/client', () => ({
  getDatabase: () => mockGetDatabase(),
  resetDatabaseForTests: jest.fn(),
}));

function createFreshDb(): SQLiteDatabase {
  const db = createMigratedDatabase();
  db.runSync("INSERT INTO categories (name) VALUES ('Bebidas')");
  db.runSync(
    'INSERT INTO products (category_id, name, selling_price, purchase_price, current_stock, min_stock, image_uri) VALUES (?, ?, ?, ?, ?, ?, ?)',
    1,
    'Coca Cola',
    100,
    60,
    10,
    2,
    null,
  );
  return db;
}

function makeItem(overrides?: Record<string, unknown>) {
  return {
    producto: {
      id: 1,
      nombre: 'Coca Cola',
      precioVenta: 100,
      precioCompra: 60,
      stockActual: 10,
      imagenUri: null,
    },
    cantidad: 5,
    descuento: 0,
    ...overrides,
  };
}

describe('registrarCompra', () => {
  let db: SQLiteDatabase;

  beforeEach(() => {
    db = createFreshDb();
    mockGetDatabase.mockReturnValue(db);
  });

  it('lanza error si carrito vacío', () => {
    expect(() =>
      registrarCompra({ items: [], tipoPago: 'EFECTIVO', detalle: null, currency: 'Bs' }),
    ).toThrow('vacío');
  });

  it('lanza error si total <= 0', () => {
    expect(() =>
      registrarCompra({
        items: [makeItem({ cantidad: 0 })],
        tipoPago: 'EFECTIVO',
        detalle: null,
        currency: 'Bs',
      }),
    ).toThrow('mayor a 0');
  });

  it('registra una compra y retorna transaccionId', () => {
    const result = registrarCompra({
      items: [makeItem()],
      tipoPago: 'EFECTIVO',
      detalle: 'Compra de prueba',
      currency: 'Bs',
    });
    expect(result.transaccionId).toBeGreaterThan(0);
    expect(result.total).toBe(300); // 5 * 60
  });

  it('incrementa stock del producto', () => {
    registrarCompra({
      items: [makeItem({ cantidad: 3 })],
      tipoPago: 'TARJETA',
      detalle: null,
      currency: 'Bs',
    });
    const row = db.getFirstSync<{ current_stock: number }>(
      'SELECT current_stock FROM products WHERE id = 1',
    );
    expect(row!.current_stock).toBe(13); // 10 + 3
  });

  it('actualiza financial_accumulators (compras+, saldo-)', () => {
    registrarCompra({
      items: [makeItem()],
      tipoPago: 'EFECTIVO',
      detalle: null,
      currency: 'Bs',
    });
    const row = db.getFirstSync<{
      total_purchases: number;
      net_balance: number;
    }>('SELECT total_purchases, net_balance FROM financial_accumulators WHERE id = 1');
    expect(row!.total_purchases).toBe(300);
    expect(row!.net_balance).toBe(-300);
  });
});
