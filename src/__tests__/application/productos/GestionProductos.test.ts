import { openDatabaseSync, type SQLiteDatabase } from 'expo-sqlite';

import { crearProducto, actualizarProducto, eliminarProducto } from '@/application/productos/GestionProductos';
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

describe('GestionProductos', () => {
  let t: TestDb;

  beforeEach(() => {
    t = createTestDb();
    t.categorias.crear({ nombre: 'Cat1' });
    mockGetRepositories.mockReturnValue({
      productos: t.productos,
      categorias: t.categorias,
      transacciones: t.transacciones,
      resumenCaja: t.resumenCaja,
    });
  });

  describe('crearProducto', () => {
    it('crea y retorna producto', () => {
      const p = crearProducto({
        categoriaId: 1,
        nombre: 'Nuevo Producto',
        precioVenta: 200,
        precioCompra: 150,
        stockActual: 5,
        stockMinimo: 1,
        imagenUri: null,
      });
      expect(p.nombre).toBe('Nuevo Producto');
      expect(p.id).toBeGreaterThan(0);
    });
  });

  describe('actualizarProducto', () => {
    it('actualiza producto existente', () => {
      const creado = crearProducto({
        categoriaId: 1,
        nombre: 'Original',
        precioVenta: 100,
        precioCompra: 50,
        stockActual: 10,
        stockMinimo: 2,
        imagenUri: null,
      });
      const actualizado = actualizarProducto(creado.id, {
        categoriaId: 1,
        nombre: 'Actualizado',
        precioVenta: 150,
        precioCompra: 80,
        stockActual: 8,
        stockMinimo: 3,
        imagenUri: null,
      });
      expect(actualizado.nombre).toBe('Actualizado');
      expect(actualizado.precioVenta).toBe(150);
    });
  });

  describe('eliminarProducto', () => {
    it('marca soft delete', () => {
      const p = crearProducto({
        categoriaId: 1,
        nombre: 'A eliminar',
        precioVenta: 50,
        precioCompra: 30,
        stockActual: 5,
        stockMinimo: 1,
        imagenUri: null,
      });
      eliminarProducto(p.id);
      expect(t.productos.findById(p.id)).toBeNull();
    });
  });
});
