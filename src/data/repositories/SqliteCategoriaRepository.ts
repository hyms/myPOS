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
      'SELECT id, nombre, fecha_registro, fecha_delete FROM categorias WHERE fecha_delete IS NULL ORDER BY nombre ASC',
    );
    return rows.map(toCategoria);
  }

  findById(id: CategoriaId): Categoria | null {
    const row = this.db.getFirstSync<CategoriaRow>(
      'SELECT id, nombre, fecha_registro, fecha_delete FROM categorias WHERE id = ? AND fecha_delete IS NULL',
      id,
    );
    return row ? toCategoria(row) : null;
  }

  crear(input: CreateCategoriaInput): Categoria {
    if (!input.nombre.trim()) {
      throw new Error('El nombre de la categoría es obligatorio.');
    }
    const result = this.db.runSync(
      'INSERT INTO categorias (nombre) VALUES (?)',
      input.nombre.trim(),
    );
    const created = this.db.getFirstSync<CategoriaRow>(
      'SELECT id, nombre, fecha_registro, fecha_delete FROM categorias WHERE id = ?',
      result.lastInsertRowId,
    );
    if (!created) {
      throw new Error('No se pudo crear la categoría.');
    }
    return toCategoria(created);
  }
}
