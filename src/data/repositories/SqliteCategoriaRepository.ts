import type { SQLiteDatabase } from 'expo-sqlite';

import type {
  Categoria,
  CategoriaId,
  CreateCategoriaInput,
} from '@/domain/entities/Categoria';
import { toCategoria, type CategoriaRow } from '@/data/mappers/rowMappers';
import type { ICategoriaRepository } from './ICategoriaRepository';

export class SqliteCategoriaRepository implements ICategoriaRepository {
  constructor(private readonly db: SQLiteDatabase) {}

  listar(): ReadonlyArray<Categoria> {
    const rows = this.db.getAllSync<CategoriaRow>(
      'SELECT id, name, registered_at, deleted_at FROM categories WHERE deleted_at IS NULL ORDER BY name ASC',
    );
    return rows.map(toCategoria);
  }

  findById(id: CategoriaId): Categoria | null {
    const row = this.db.getFirstSync<CategoriaRow>(
      'SELECT id, name, registered_at, deleted_at FROM categories WHERE id = ? AND deleted_at IS NULL',
      id,
    );
    return row ? toCategoria(row) : null;
  }

  crear(input: CreateCategoriaInput): Categoria {
    if (!input.nombre.trim()) {
      throw new Error('El nombre de la categoría es obligatorio.');
    }
    const result = this.db.runSync(
      'INSERT INTO categories (name) VALUES (?)',
      input.nombre.trim(),
    );
    const created = this.db.getFirstSync<CategoriaRow>(
      'SELECT id, name, registered_at, deleted_at FROM categories WHERE id = ?',
      result.lastInsertRowId,
    );
    if (!created) {
      throw new Error('No se pudo crear la categoría.');
    }
    return toCategoria(created);
  }
}
