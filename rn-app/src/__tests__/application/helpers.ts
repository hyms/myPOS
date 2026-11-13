import { openDatabaseSync, type SQLiteDatabase } from 'expo-sqlite';

import { SqliteProductoRepository } from '@/data/repositories/SqliteProductoRepository';
import { SqliteCategoriaRepository } from '@/data/repositories/SqliteCategoriaRepository';
import { SqliteTransaccionRepository } from '@/data/repositories/SqliteTransaccionRepository';
import { SqliteResumenCajaRepository } from '@/data/repositories/SqliteResumenCajaRepository';
import { SCHEMA_SQL, SEED_SQL } from '@/data/database/schema.sql';

export interface TestDb {
  db: SQLiteDatabase;
  productos: SqliteProductoRepository;
  categorias: SqliteCategoriaRepository;
  transacciones: SqliteTransaccionRepository;
  resumenCaja: SqliteResumenCajaRepository;
}

export function createTestDb(): TestDb {
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

export function seedCategoria(testDb: TestDb, nombre = 'Test Cat'): number {
  return testDb.categorias.crear({ nombre }).id;
}

export function seedProducto(
  testDb: TestDb,
  overrides?: Partial<{
    categoriaId: number;
    nombre: string;
    precioVenta: number;
    precioCompra: number;
    stockActual: number;
    stockMinimo: number;
    imagenUri: string | null;
  }>,
): { id: number; nombre: string; stockActual: number } {
  const p = testDb.productos.crear({
    categoriaId: overrides?.categoriaId ?? 1,
    nombre: overrides?.nombre ?? 'Producto Test',
    precioVenta: overrides?.precioVenta ?? 100,
    precioCompra: overrides?.precioCompra ?? 60,
    stockActual: overrides?.stockActual ?? 10,
    stockMinimo: overrides?.stockMinimo ?? 2,
    imagenUri: overrides?.imagenUri ?? null,
  });
  return { id: p.id, nombre: p.nombre, stockActual: p.stockActual };
}
