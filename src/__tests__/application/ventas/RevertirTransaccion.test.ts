import { openDatabaseSync, type SQLiteDatabase } from 'expo-sqlite';

import { revertirTransaccion } from '@/application/ventas/RevertirTransaccion';
import { SqliteProductoRepository } from '@/data/repositories/SqliteProductoRepository';
import { SqliteCategoriaRepository } from '@/data/repositories/SqliteCategoriaRepository';
import { SqliteTransaccionRepository } from '@/data/repositories/SqliteTransaccionRepository';
import { SqliteResumenCajaRepository } from '@/data/repositories/SqliteResumenCajaRepository';
import { SCHEMA_SQL, SEED_SQL } from '@/data/database/schema.sql';

const mockGetDatabase = jest.fn();
const mockGetRepositories = jest.fn();

jest.mock('@/data/database/client', () => ({
  getDatabase: () => mockGetDatabase(),
  resetDatabaseForTests: jest.fn(),
}));

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

function seedData(t: TestDb) {
  t.categorias.crear({ nombre: 'Cat1' });
  t.productos.crear({
    categoriaId: 1,
    nombre: 'Producto A',
    precioVenta: 100,
    precioCompra: 60,
    stockActual: 20,
    stockMinimo: 2,
    imagenUri: null,
  });
}

function createVenta(t: TestDb): number {
  const trx = t.db.runSync(
    "INSERT INTO transacciones (tipo, monto_total, tipo_pago) VALUES ('VENTA', ?, ?)",
    200,
    'EFECTIVO',
  );
  const id = trx.lastInsertRowId;
  t.db.runSync(
    'INSERT INTO detalle_transacciones (transaccion_id, producto_id, cantidad, precio_unitario) VALUES (?, ?, ?, ?)',
    id,
    1,
    2,
    100,
  );
  t.db.runSync(
    'UPDATE productos SET stock_actual = stock_actual - ? WHERE id = ?',
    2,
    1,
  );
  t.db.runSync(
    'UPDATE resumen_caja SET total_ventas = total_ventas + ?, saldo_actual = saldo_actual + ? WHERE id = 1',
    200,
    200,
  );
  return id;
}

function createCompra(t: TestDb): number {
  const trx = t.db.runSync(
    "INSERT INTO transacciones (tipo, monto_total, tipo_pago) VALUES ('COMPRA', ?, ?)",
    300,
    'EFECTIVO',
  );
  const id = trx.lastInsertRowId;
  t.db.runSync(
    'INSERT INTO detalle_transacciones (transaccion_id, producto_id, cantidad, precio_unitario) VALUES (?, ?, ?, ?)',
    id,
    1,
    5,
    60,
  );
  t.db.runSync(
    'UPDATE productos SET stock_actual = stock_actual + ? WHERE id = ?',
    5,
    1,
  );
  t.db.runSync(
    'UPDATE resumen_caja SET total_compras = total_compras + ?, saldo_actual = saldo_actual - ? WHERE id = 1',
    300,
    300,
  );
  return id;
}

function createGasto(t: TestDb): number {
  const trx = t.db.runSync(
    "INSERT INTO transacciones (tipo, monto_total, tipo_pago, detalle) VALUES ('GASTO', ?, ?, ?)",
    50,
    'EFECTIVO',
    'Luz',
  );
  const id = trx.lastInsertRowId;
  t.db.runSync(
    'UPDATE resumen_caja SET total_gastos = total_gastos + ?, saldo_actual = saldo_actual - ? WHERE id = 1',
    50,
    50,
  );
  return id;
}

describe('revertirTransaccion', () => {
  let t: TestDb;

  beforeEach(() => {
    t = createTestDb();
    seedData(t);
    mockGetDatabase.mockReturnValue(t.db);
    mockGetRepositories.mockReturnValue({
      productos: t.productos,
      categorias: t.categorias,
      transacciones: t.transacciones,
      resumenCaja: t.resumenCaja,
    });
  });

  describe('VENTA', () => {
    it('revierte una venta: restaura stock y caja', () => {
      const id = createVenta(t);
      const result = revertirTransaccion(id);

      expect(result.transaccionId).toBe(id);
      expect(result.tipo).toBe('VENTA');

      // Stock restaurado
      const prod = t.productos.findById(1)!;
      expect(prod.stockActual).toBe(20);

      // Caja revertida
      const caja = t.resumenCaja.obtener();
      expect(caja.totalVentas).toBe(0);
      expect(caja.saldoActual).toBe(0);

      // Transacción marcada como borrada (fecha_delete seteada)
      const trx = t.transacciones.findById(id);
      expect(trx!.fechaDelete).not.toBeNull();
    });
  });

  describe('COMPRA', () => {
    it('revierte una compra: descuenta stock y restaura caja', () => {
      const id = createCompra(t);
      const result = revertirTransaccion(id);

      expect(result.tipo).toBe('COMPRA');

      // Stock reducido
      const prod = t.productos.findById(1)!;
      expect(prod.stockActual).toBe(20);

      // Caja revertida
      const caja = t.resumenCaja.obtener();
      expect(caja.totalCompras).toBe(0);
      expect(caja.saldoActual).toBe(0);
    });

    it('lanza error si revertir compra dejaría stock negativo', () => {
      const id = createCompra(t);
      // Producto actualmente tiene stock 20 después de la compra
      // Si vendemos 25 unidades para reducir stock a menos de la cantidad a revertir
      t.db.runSync('UPDATE productos SET stock_actual = ? WHERE id = 1', 3);

      expect(() => revertirTransaccion(id)).toThrow('stock negativo');
    });
  });

  describe('GASTO', () => {
    it('revierte un gasto: restaura caja', () => {
      const id = createGasto(t);
      const result = revertirTransaccion(id);

      expect(result.tipo).toBe('GASTO');

      const caja = t.resumenCaja.obtener();
      expect(caja.totalGastos).toBe(0);
      expect(caja.saldoActual).toBe(0);
    });
  });

  it('lanza error si la transacción no existe', () => {
    expect(() => revertirTransaccion(999)).toThrow('no existe');
  });

  it('lanza error si la transacción ya fue revertida', () => {
    const id = createVenta(t);
    revertirTransaccion(id);
    expect(() => revertirTransaccion(id)).toThrow('ya fue revertida');
  });
});
