import { openDatabaseSync, type SQLiteDatabase } from 'expo-sqlite';

import { registrarVenta } from '@/application/ventas/RegistrarVenta';
import { SCHEMA_SQL, SEED_SQL } from '@/data/database/schema.sql';

const mockGetDatabase = jest.fn();

jest.mock('@/data/database/client', () => ({
  getDatabase: () => mockGetDatabase(),
  resetDatabaseForTests: jest.fn(),
}));

function createFreshDb(): SQLiteDatabase {
  const db = openDatabaseSync(':memory:');
  db.execSync(SCHEMA_SQL);
  db.execSync(SEED_SQL);
  db.runSync("INSERT INTO categorias (nombre) VALUES ('Bebidas')");
  db.runSync(
    'INSERT INTO productos (categoria_id, nombre, precio_venta, precio_compra, stock_actual, stock_minimo, imagen_uri) VALUES (?, ?, ?, ?, ?, ?, ?)',
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
      registrarVenta({ items: [], tipoPago: 'EFECTIVO', detalle: null }),
    ).toThrow('vacío');
  });

  it('lanza error si total <= 0', () => {
    expect(() =>
      registrarVenta({
        items: [makeItem({ cantidad: 0 })],
        tipoPago: 'EFECTIVO',
        detalle: null,
      }),
    ).toThrow('mayor a 0');
  });

  it('lanza error si stock insuficiente', () => {
    expect(() =>
      registrarVenta({
        items: [makeItem({ cantidad: 999 })],
        tipoPago: 'EFECTIVO',
        detalle: null,
      }),
    ).toThrow('Stock insuficiente');
  });

  it('registra una venta y retorna transaccionId', () => {
    const result = registrarVenta({
      items: [makeItem()],
      tipoPago: 'EFECTIVO',
      detalle: 'Venta de prueba',
    });
    expect(result.transaccionId).toBeGreaterThan(0);
    expect(result.total).toBe(200);
  });

  it('descuenta stock del producto', () => {
    registrarVenta({
      items: [makeItem({ cantidad: 3 })],
      tipoPago: 'TARJETA',
      detalle: null,
    });
    const row = db.getFirstSync<{ stock_actual: number }>(
      'SELECT stock_actual FROM productos WHERE id = 1',
    );
    expect(row!.stock_actual).toBe(7);
  });

  it('actualiza resumen_caja', () => {
    registrarVenta({
      items: [makeItem()],
      tipoPago: 'EFECTIVO',
      detalle: null,
    });
    const row = db.getFirstSync<{
      total_ventas: number;
      saldo_actual: number;
    }>('SELECT total_ventas, saldo_actual FROM resumen_caja WHERE id = 1');
    expect(row!.total_ventas).toBe(200);
    expect(row!.saldo_actual).toBe(200);
  });

  it('aplica descuento por ítem', () => {
    const result = registrarVenta({
      items: [makeItem({ descuento: 10 })],
      tipoPago: 'EFECTIVO',
      detalle: null,
    });
    expect(result.total).toBe(190);
  });

  it('detecta stock mínimo (stock alert)', () => {
    // Set stock to just above minimum, then sell enough to go below
    db.runSync('UPDATE productos SET stock_actual = 3 WHERE id = 1');
    const result = registrarVenta({
      items: [makeItem({ cantidad: 2 })],
      tipoPago: 'EFECTIVO',
      detalle: null,
    });
    // stock goes to 1, min is 2 => alert
    expect(result.stockAlerts).toHaveLength(1);
    expect(result.stockAlerts[0]!.nombre).toBe('Coca Cola');
  });
});
