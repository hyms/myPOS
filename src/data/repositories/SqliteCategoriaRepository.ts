import type { SQLiteDatabase } from 'expo-sqlite';

import type {
  Categoria,
  CategoriaId,
  CreateCategoriaInput,
} from '@/domain/entities/Categoria';
import { toCategoria, type CategoriaRow } from '@/data/mappers/rowMappers';
import { normalizeCategoryName } from '@/domain/factories/CategoryFactory';
import type { ICategoriaRepository } from './ICategoriaRepository';

const CATEGORY_COLUMNS = 'id, name, registered_at, deleted_at';

export class SqliteCategoriaRepository implements ICategoriaRepository {
  constructor(private readonly db: SQLiteDatabase) {}

  listar(): ReadonlyArray<Categoria> {
    const rows = this.db.getAllSync<CategoriaRow>(
      `SELECT ${CATEGORY_COLUMNS} FROM categories WHERE deleted_at IS NULL ORDER BY name ASC`,
    );
    return rows.map(toCategoria);
  }

  findById(id: CategoriaId): Categoria | null {
    const row = this.db.getFirstSync<CategoriaRow>(
      `SELECT ${CATEGORY_COLUMNS} FROM categories WHERE id = ? AND deleted_at IS NULL`,
      id,
    );
    return row ? toCategoria(row) : null;
  }

  crear(input: CreateCategoriaInput): Categoria {
    const name = normalizeCategoryName(input.nombre);
    let createdId = 0;
    this.db.withTransactionSync(() => {
      const result = this.db.runSync('INSERT INTO categories (name) VALUES (?)', name);
      createdId = Number(result.lastInsertRowId);
    });
    const created = this.db.getFirstSync<CategoriaRow>(
      `SELECT ${CATEGORY_COLUMNS} FROM categories WHERE id = ?`,
      createdId,
    );
    if (!created) {
      throw new Error('No se pudo crear la categoría.');
    }
    return toCategoria(created);
  }
}
