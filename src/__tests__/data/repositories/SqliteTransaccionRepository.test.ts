import { openDatabaseSync, type SQLiteDatabase } from 'expo-sqlite';

import { SqliteTransaccionRepository } from '@/data/repositories/SqliteTransaccionRepository';
import { SCHEMA_SQL } from '@/data/database/schema.sql';

describe('SqliteTransaccionRepository', () => {
  let db: SQLiteDatabase;
  let repo: SqliteTransaccionRepository;

  beforeEach(() => {
    db = openDatabaseSync(':memory:');
    db.execSync(SCHEMA_SQL);
    repo = new SqliteTransaccionRepository(db);
    // Seed categorias and productos
    db.runSync("INSERT INTO categorias (nombre) VALUES ('Cat1')");
    db.runSync(
      "INSERT INTO productos (categoria_id, nombre, precio_venta, precio_compra) VALUES (1, 'Prod1', 100, 60)",
    );
  });

  function insertVenta(monto = 100, tipoPago = 'EFECTIVO') {
    return db.runSync(
      "INSERT INTO transacciones (tipo, monto_total, tipo_pago) VALUES ('VENTA', ?, ?)",
      monto,
      tipoPago,
    ).lastInsertRowId;
  }

  function insertDetalle(transaccionId: number, productoId = 1, cantidad = 1, precio = 100) {
    db.runSync(
      'INSERT INTO detalle_transacciones (transaccion_id, producto_id, cantidad, precio_unitario) VALUES (?, ?, ?, ?)',
      transaccionId,
      productoId,
      cantidad,
      precio,
    );
  }

  describe('findById', () => {
    it('retorna null si no existe', () => {
      expect(repo.findById(999)).toBeNull();
    });

    it('retorna transacción por id', () => {
      const id = insertVenta();
      const trx = repo.findById(id);
      expect(trx).not.toBeNull();
      expect(trx!.tipo).toBe('VENTA');
      expect(trx!.montoTotal).toBe(100);
    });
  });

  describe('findDetalles', () => {
    it('retorna detalles de una transacción', () => {
      const trxId = insertVenta();
      insertDetalle(trxId);
      const detalles = repo.findDetalles(trxId);
      expect(detalles).toHaveLength(1);
      expect(detalles[0]!.transaccionId).toBe(trxId);
    });

    it('retorna array vacío si no hay detalles', () => {
      const trxId = insertVenta();
      expect(repo.findDetalles(trxId)).toEqual([]);
    });
  });

  describe('listar', () => {
    it('retorna transacciones paginadas', () => {
      insertVenta(100);
      insertVenta(200);
      const result = repo.listar({ limit: 1, offset: 0 });
      expect(result).toHaveLength(1);
    });

    it('filtra por tipo', () => {
      insertVenta(100);
      const compraId = db.runSync(
        "INSERT INTO transacciones (tipo, monto_total, tipo_pago) VALUES ('COMPRA', ?, ?)",
        50,
        'EFECTIVO',
      ).lastInsertRowId;
      const result = repo.listar({ tipo: 'COMPRA', limit: 10, offset: 0 });
      expect(result).toHaveLength(1);
      expect(result[0]!.id).toBe(compraId);
    });
  });

  describe('listarPorMesAnio', () => {
    it('retorna totales agrupados por tipo', () => {
      insertVenta(100);
      insertVenta(200);
      db.runSync(
        "INSERT INTO transacciones (tipo, monto_total, tipo_pago) VALUES ('COMPRA', ?, ?)",
        50,
        'EFECTIVO',
      );
      const result = repo.listarPorMesAnio({ periodo: 'YEAR' });
      expect(result).toHaveLength(2);
      const ventaTotal = result.find((r) => r.tipo === 'VENTA')?.total ?? 0;
      const compraTotal = result.find((r) => r.tipo === 'COMPRA')?.total ?? 0;
      expect(ventaTotal).toBe(300);
      expect(compraTotal).toBe(50);
    });
  });

  describe('gastosDelMes', () => {
    it('retorna solo gastos del mes actual', () => {
      insertVenta(100);
      db.runSync(
        "INSERT INTO transacciones (tipo, monto_total, tipo_pago) VALUES ('GASTO', ?, ?)",
        30,
        'EFECTIVO',
      );
      const gastos = repo.gastosDelMes(10, 0);
      expect(gastos).toHaveLength(1);
      expect(gastos[0]!.tipo).toBe('GASTO');
      expect(gastos[0]!.montoTotal).toBe(30);
    });
  });
});
