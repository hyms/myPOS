import { type SQLiteDatabase } from 'expo-sqlite';

import { registrarVenta } from '@/application/ventas/RegistrarVenta';
import { StockError } from '@/domain/rules/stockRules';
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
    cantidad: 2,
    descuento: 0,
    ...overrides,
  };
}

describe('registrarVenta', () => {
  let db: SQLiteDatabase;

  beforeEach(() => {
    db = createFreshDb();
    mockGetDatabase.mockReturnValue(db);
  });

  it('lanza error si carrito vacío', () => {
    expect(() =>
      registrarVenta({ items: [], tipoPago: 'EFECTIVO', detalle: null, currency: 'Bs' }),
    ).toThrow('vacío');
  });

  it('lanza error si total <= 0', () => {
    expect(() =>
      registrarVenta({
        items: [makeItem({ cantidad: 0 })],
        tipoPago: 'EFECTIVO',
        detalle: null,
        currency: 'Bs',
      }),
    ).toThrow('mayor a 0');
  });

  it('lanza error si stock insuficiente', () => {
    expect(() =>
      registrarVenta({
        items: [makeItem({ cantidad: 999 })],
        tipoPago: 'EFECTIVO',
        detalle: null,
        currency: 'Bs',
      }),
    ).toThrow(StockError);
  });

  it('registra una venta y retorna transaccionId', () => {
    const result = registrarVenta({
      items: [makeItem()],
      tipoPago: 'EFECTIVO',
      detalle: 'Venta de prueba',
      currency: 'Bs',
    });
    expect(result.transaccionId).toBeGreaterThan(0);
    expect(result.total).toBe(200);
  });

  it('descuenta stock del producto', () => {
    registrarVenta({
      items: [makeItem({ cantidad: 3 })],
      tipoPago: 'TARJETA',
      detalle: null,
      currency: 'Bs',
    });
    const row = db.getFirstSync<{ current_stock: number }>(
      'SELECT current_stock FROM products WHERE id = 1',
    );
    expect(row!.current_stock).toBe(7);
  });

  it('actualiza financial_accumulators', () => {
    registrarVenta({
      items: [makeItem()],
      tipoPago: 'EFECTIVO',
      detalle: null,
      currency: 'Bs',
    });
    const row = db.getFirstSync<{
      total_sales: number;
      net_balance: number;
    }>('SELECT total_sales, net_balance FROM financial_accumulators WHERE id = 1');
    expect(row!.total_sales).toBe(200);
    expect(row!.net_balance).toBe(200);
  });

  it('aplica descuento por ítem', () => {
    const result = registrarVenta({
      items: [makeItem({ descuento: 10 })],
      tipoPago: 'EFECTIVO',
      detalle: null,
      currency: 'Bs',
    });
    expect(result.total).toBe(190);
  });

  it('detecta stock mínimo (stock alert)', () => {
    // Set stock to just above minimum, then sell enough to go below
    db.runSync('UPDATE products SET current_stock = 3 WHERE id = 1');
    const result = registrarVenta({
      items: [makeItem({ cantidad: 2 })],
      tipoPago: 'EFECTIVO',
      detalle: null,
      currency: 'Bs',
    });
    // stock goes to 1, min is 2 => alert
    expect(result.stockAlerts).toHaveLength(1);
    expect(result.stockAlerts[0]!.nombre).toBe('Coca Cola');
  });
});
