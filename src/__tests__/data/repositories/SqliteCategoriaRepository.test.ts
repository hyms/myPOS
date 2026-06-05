import { openDatabaseSync, type SQLiteDatabase } from 'expo-sqlite';

import { SqliteCategoriaRepository } from '@/data/repositories/SqliteCategoriaRepository';

describe('SqliteCategoriaRepository', () => {
  let db: SQLiteDatabase;
  let repo: SqliteCategoriaRepository;

  beforeEach(() => {
    db = openDatabaseSync(':memory:');
    db.execSync(`
      CREATE TABLE IF NOT EXISTS categorias (
        id INTEGER PRIMARY KEY AUTOINCREMENT,
        nombre TEXT NOT NULL,
        fecha_registro TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
        fecha_delete TIMESTAMP DEFAULT NULL
      )
    `);
    repo = new SqliteCategoriaRepository(db);
  });

  describe('listar', () => {
    it('retorna array vacío si no hay categorías', () => {
      expect(repo.listar()).toEqual([]);
    });

    it('retorna categorías activas ordenadas por nombre', () => {
      db.runSync('INSERT INTO categorias (nombre) VALUES (?)', 'Zapatos');
      db.runSync('INSERT INTO categorias (nombre) VALUES (?)', 'Bebidas');
      db.runSync('INSERT INTO categorias (nombre) VALUES (?)', 'Abarrotes');

      const result = repo.listar();
      expect(result.map((c) => c.nombre)).toEqual(['Abarrotes', 'Bebidas', 'Zapatos']);
    });

    it('no retorna categorías con fecha_delete', () => {
      db.runSync('INSERT INTO categorias (nombre) VALUES (?)', 'Activa');
      db.runSync('INSERT INTO categorias (nombre, fecha_delete) VALUES (?, ?)', 'Borrada', '2024-01-01');

      expect(repo.listar()).toHaveLength(1);
      expect(repo.listar()[0]?.nombre).toBe('Activa');
    });
  });

  describe('findById', () => {
    it('retorna null si no existe', () => {
      expect(repo.findById(999)).toBeNull();
    });

    it('retorna la categoría por id', () => {
      db.runSync('INSERT INTO categorias (nombre) VALUES (?)', 'Lácteos');
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
