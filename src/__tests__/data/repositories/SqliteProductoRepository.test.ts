import { openDatabaseSync, type SQLiteDatabase } from 'expo-sqlite';

import { SqliteProductoRepository } from '@/data/repositories/SqliteProductoRepository';
import { SCHEMA_SQL, SEED_SQL } from '@/data/database/schema.sql';

describe('SqliteProductoRepository', () => {
  let db: SQLiteDatabase;
  let repo: SqliteProductoRepository;

  beforeEach(() => {
    db = openDatabaseSync(':memory:');
    db.execSync(SCHEMA_SQL);
    db.execSync(SEED_SQL);
    db.runSync("INSERT INTO categorias (nombre) VALUES ('Bebidas')");
    repo = new SqliteProductoRepository(db);
  });

  const makeInput = (overrides?: Record<string, unknown>) => ({
    categoriaId: 1,
    nombre: 'Coca Cola',
    precioVenta: 100,
    precioCompra: 60,
    stockActual: 10,
    stockMinimo: 2,
    imagenUri: null,
    ...overrides,
  });

  describe('crear', () => {
    it('crea un producto y retorna sus datos', () => {
      const p = repo.crear(makeInput());
      expect(p.nombre).toBe('Coca Cola');
      expect(p.precioVenta).toBe(100);
      expect(p.precioCompra).toBe(60);
      expect(p.stockActual).toBe(10);
      expect(p.stockMinimo).toBe(2);
      expect(p.categoriaId).toBe(1);
      expect(p.id).toBeGreaterThan(0);
    });

    it('lanza error si la categoría no existe', () => {
      expect(() => repo.crear(makeInput({ categoriaId: 999 }))).toThrow();
    });

    it('lanza error si nombre vacío', () => {
      expect(() => repo.crear(makeInput({ nombre: '' }))).toThrow('obligatorio');
    });

    it('lanza error si precios negativos', () => {
      expect(() => repo.crear(makeInput({ precioVenta: -1 }))).toThrow('negativos');
      expect(() => repo.crear(makeInput({ precioCompra: -5 }))).toThrow('negativos');
    });

    it('lanza error si stock negativo', () => {
      expect(() => repo.crear(makeInput({ stockActual: -1 }))).toThrow('negativo');
      expect(() => repo.crear(makeInput({ stockMinimo: -1 }))).toThrow('negativo');
    });

    it('lanza error si categoriaId es null/undefined', () => {
      expect(() => repo.crear(makeInput({ categoriaId: null, nombre: 'Test' }))).toThrow(
        'categoría es obligatoria',
      );
    });

    it('limpia espacios del nombre', () => {
      const p = repo.crear(makeInput({ nombre: '  Fanta   ' }));
      expect(p.nombre).toBe('Fanta');
    });
  });

  describe('findById', () => {
    it('retorna null si no existe o borrado', () => {
      expect(repo.findById(999)).toBeNull();
    });

    it('retorna el producto por id', () => {
      const created = repo.crear(makeInput({ nombre: 'Sprite' }));
      const found = repo.findById(created.id);
      expect(found).not.toBeNull();
      expect(found!.nombre).toBe('Sprite');
    });

    it('no retorna productos con soft delete', () => {
      const p = repo.crear(makeInput());
      repo.eliminar(p.id);
      expect(repo.findById(p.id)).toBeNull();
    });
  });

  describe('listar', () => {
    it('retorna productos paginados', () => {
      repo.crear(makeInput({ nombre: 'A' }));
      repo.crear(makeInput({ nombre: 'B' }));
      repo.crear(makeInput({ nombre: 'C' }));

      const result = repo.listar({ sort: 'NOMBRE_ASC', limit: 2, offset: 0 });
      expect(result).toHaveLength(2);
    });

    it('filtra por categoría', () => {
      db.runSync("INSERT INTO categorias (nombre) VALUES ('Lácteos')");
      repo.crear(makeInput({ nombre: 'Coca', categoriaId: 1 }));
      repo.crear(makeInput({ nombre: 'Leche', categoriaId: 2 }));

      const result = repo.listar({ sort: 'NOMBRE_ASC', limit: 10, offset: 0, categoriaId: 2 });
      expect(result).toHaveLength(1);
      expect(result[0]!.nombre).toBe('Leche');
    });

    it('busca por texto', () => {
      repo.crear(makeInput({ nombre: 'Coca Cola' }));
      repo.crear(makeInput({ nombre: 'Fanta Naranja' }));

      const result = repo.listar({ sort: 'NOMBRE_ASC', limit: 10, offset: 0, search: 'coca' });
      expect(result).toHaveLength(1);
      expect(result[0]!.nombre).toBe('Coca Cola');
    });
  });

  describe('actualizar', () => {
    it('actualiza campos del producto', () => {
      const p = repo.crear(makeInput());
      const updated = repo.actualizar(p.id, makeInput({ nombre: 'Coca Zero', precioVenta: 120 }));
      expect(updated.nombre).toBe('Coca Zero');
      expect(updated.precioVenta).toBe(120);
    });

    it('lanza error si producto no existe', () => {
      expect(() => repo.actualizar(999, makeInput())).toThrow('No se pudo actualizar');
    });
  });

  describe('eliminar', () => {
    it('marca fecha_delete (soft delete)', () => {
      const p = repo.crear(makeInput());
      repo.eliminar(p.id);
      expect(repo.findById(p.id)).toBeNull();

      // Verify it's still in DB
      const row = db.getFirstSync<{ id: number; fecha_delete: string | null }>(
        'SELECT id, fecha_delete FROM productos WHERE id = ?',
        p.id,
      );
      expect(row).not.toBeNull();
      expect(row!.fecha_delete).not.toBeNull();
    });
  });

  describe('findByIds', () => {
    it('retorna productos por lista de ids', () => {
      const a = repo.crear(makeInput({ nombre: 'A' }));
      const b = repo.crear(makeInput({ nombre: 'B' }));
      const result = repo.findByIds([a.id, b.id]);
      expect(result).toHaveLength(2);
    });

    it('retorna array vacío si lista vacía', () => {
      expect(repo.findByIds([])).toEqual([]);
    });

    it('omite productos borrados', () => {
      const a = repo.crear(makeInput({ nombre: 'A' }));
      const b = repo.crear(makeInput({ nombre: 'B' }));
      repo.eliminar(b.id);
      const result = repo.findByIds([a.id, b.id]);
      expect(result).toHaveLength(1);
    });
  });
});
