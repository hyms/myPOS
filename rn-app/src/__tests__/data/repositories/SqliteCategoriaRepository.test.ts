import { type SQLiteDatabase } from 'expo-sqlite';

import { SqliteCategoriaRepository } from '@/data/repositories/SqliteCategoriaRepository';
import { createMigratedDatabase } from '@/__tests__/support/database';

describe('SqliteCategoriaRepository', () => {
  let db: SQLiteDatabase;
  let repo: SqliteCategoriaRepository;

  beforeEach(() => {
    db = createMigratedDatabase();
    repo = new SqliteCategoriaRepository(db);
  });

  describe('listar', () => {
    it('retorna array vacío si no hay categorías', () => {
      expect(repo.listar()).toEqual([]);
    });

    it('retorna categorías activas ordenadas por nombre', () => {
      db.runSync('INSERT INTO categories (name) VALUES (?)', 'Zapatos');
      db.runSync('INSERT INTO categories (name) VALUES (?)', 'Bebidas');
      db.runSync('INSERT INTO categories (name) VALUES (?)', 'Abarrotes');

      const result = repo.listar();
      expect(result.map((c) => c.nombre)).toEqual(['Abarrotes', 'Bebidas', 'Zapatos']);
    });

    it('no retorna categorías con deleted_at', () => {
      db.runSync('INSERT INTO categories (name) VALUES (?)', 'Activa');
      db.runSync('INSERT INTO categories (name, deleted_at) VALUES (?, ?)', 'Borrada', '2024-01-01');

      expect(repo.listar()).toHaveLength(1);
      expect(repo.listar()[0]?.nombre).toBe('Activa');
    });
  });

  describe('findById', () => {
    it('retorna null si no existe', () => {
      expect(repo.findById(999)).toBeNull();
    });

    it('retorna la categoría por id', () => {
      db.runSync('INSERT INTO categories (name) VALUES (?)', 'Lácteos');
      const cat = repo.findById(1);
      expect(cat).not.toBeNull();
      expect(cat!.nombre).toBe('Lácteos');
    });
  });

  describe('crear', () => {
    it('crea y retorna la categoría', () => {
      const cat = repo.crear({ nombre: '  Carnes  ' });
      expect(cat.nombre).toBe('Carnes');
      expect(cat.id).toBeGreaterThan(0);
      expect(cat.fechaDelete).toBeNull();
    });

    it('lanza error si nombre vacío', () => {
      expect(() => repo.crear({ nombre: '' })).toThrow('obligatorio');
      expect(() => repo.crear({ nombre: '   ' })).toThrow('obligatorio');
    });
  });
});
