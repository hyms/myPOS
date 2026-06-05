import { openDatabaseSync, type SQLiteDatabase } from 'expo-sqlite';

import { registrarCompra } from '@/application/compras/RegistrarCompra';
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
      registrarCompra({ items: [], tipoPago: 'EFECTIVO', detalle: null }),
    ).toThrow('vacío');
  });

  it('lanza error si total <= 0', () => {
    expect(() =>
      registrarCompra({
        items: [makeItem({ cantidad: 0 })],
        tipoPago: 'EFECTIVO',
        detalle: null,
      }),
    ).toThrow('mayor a 0');
  });

  it('registra una compra y retorna transaccionId', () => {
    const result = registrarCompra({
      items: [makeItem()],
      tipoPago: 'EFECTIVO',
      detalle: 'Compra de prueba',
    });
    expect(result.transaccionId).toBeGreaterThan(0);
    expect(result.total).toBe(300); // 5 * 60
  });

  it('incrementa stock del producto', () => {
    registrarCompra({
      items: [makeItem({ cantidad: 3 })],
      tipoPago: 'TARJETA',
      detalle: null,
    });
    const row = db.getFirstSync<{ stock_actual: number }>(
      'SELECT stock_actual FROM productos WHERE id = 1',
    );
    expect(row!.stock_actual).toBe(13); // 10 + 3
  });

  it('actualiza resumen_caja (compras+, saldo-)', () => {
    registrarCompra({
      items: [makeItem()],
      tipoPago: 'EFECTIVO',
      detalle: null,
    });
    const row = db.getFirstSync<{
      total_compras: number;
      saldo_actual: number;
    }>('SELECT total_compras, saldo_actual FROM resumen_caja WHERE id = 1');
    expect(row!.total_compras).toBe(300);
    expect(row!.saldo_actual).toBe(-300);
  });
});
