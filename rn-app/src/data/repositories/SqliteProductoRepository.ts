import type { SQLiteDatabase } from 'expo-sqlite';

import type {
  CategoriaId,
} from '@/domain/entities/Categoria';
import type {
  CreateProductoInput,
  Producto,
  ProductoId,
  UpdateProductoInput,
} from '@/domain/entities/Producto';
import { normalizeProductWrite, normalizeSku } from '@/domain/factories/ProductFactory';
import { toProducto, type ProductoPopularidadRow, type ProductoRow } from '@/data/mappers/rowMappers';
import type { IProductoRepository, ProductoQuery } from './IProductoRepository';

const PRODUCT_COLUMNS =
  'id, category_id, sku, name, selling_price, purchase_price, current_stock, min_stock, image_uri, registered_at, updated_at, deleted_at';

export class SqliteProductoRepository implements IProductoRepository {
  constructor(private readonly db: SQLiteDatabase) {}

  listar(query: ProductoQuery): ReadonlyArray<Producto & { popularidad: number }> {
    const searchLike = `%${(query.search ?? '').trim()}%`;
    const hasCategoria = query.categoriaId !== undefined && query.categoriaId !== null;
    const orderClause = query.sort === 'POPULARIDAD_DESC'
      ? 'popularidad DESC, p.name ASC'
      : 'p.name ASC';

    const rows = this.db.getAllSync<ProductoPopularidadRow>(
      `SELECT p.id, p.category_id, p.sku, p.name, p.selling_price, p.purchase_price,
              p.current_stock, p.min_stock, p.image_uri, p.registered_at,
              p.updated_at, p.deleted_at,
              COALESCE(SUM(d.quantity), 0) AS popularidad
         FROM products p
         LEFT JOIN transaction_details d ON d.product_id = p.id
         LEFT JOIN sales_transactions t
           ON t.id = d.transaction_id
          AND t.type = 'VENTA'
          AND t.deleted_at IS NULL
        WHERE p.deleted_at IS NULL
          AND (? IS NULL OR p.category_id = ?)
          AND p.name LIKE ?
        GROUP BY p.id
        ORDER BY ${orderClause}
        LIMIT ? OFFSET ?`,
      hasCategoria ? query.categoriaId : null,
      hasCategoria ? query.categoriaId : null,
      searchLike,
      query.limit,
      query.offset,
    );
    return rows.map((row) => ({ ...toProducto(row), popularidad: row.popularidad }));
  }

  findById(id: ProductoId): Producto | null {
    const row = this.db.getFirstSync<ProductoRow>(
      `SELECT ${PRODUCT_COLUMNS} FROM products WHERE id = ? AND deleted_at IS NULL`,
      id,
    );
    return row ? toProducto(row) : null;
  }

  findBySku(sku: string): Producto | null {
    const normalized = normalizeSku(sku);
    if (!normalized) return null;
    const row = this.db.getFirstSync<ProductoRow>(
      `SELECT ${PRODUCT_COLUMNS} FROM products WHERE sku = ? AND deleted_at IS NULL`,
      normalized,
    );
    return row ? toProducto(row) : null;
  }

  findByIds(ids: ReadonlyArray<ProductoId>): ReadonlyArray<Producto> {
    if (ids.length === 0) return [];
    const placeholders = ids.map(() => '?').join(',');
    const rows = this.db.getAllSync<ProductoRow>(
      `SELECT ${PRODUCT_COLUMNS}
         FROM products
        WHERE id IN (${placeholders}) AND deleted_at IS NULL`,
      ...ids,
    );
    return rows.map(toProducto);
  }

  crear(input: CreateProductoInput): Producto {
    const data = normalizeProductWrite(input);
    let createdId = 0;
    this.db.withTransactionSync(() => {
      this.assertCategoryExists(data.categoriaId);
      this.assertSkuAvailable(data.sku, null);
      const result = this.db.runSync(
        `INSERT INTO products
          (category_id, sku, name, selling_price, purchase_price, current_stock, min_stock, image_uri)
         VALUES (?, ?, ?, ?, ?, ?, ?, ?)`,
        data.categoriaId,
        data.sku,
        data.nombre,
        data.precioVenta,
        data.precioCompra,
        data.stockActual,
        data.stockMinimo,
        data.imagenUri,
      );
      createdId = Number(result.lastInsertRowId);
    });
    const created = this.findById(createdId);
    if (!created) {
      throw new Error('No se pudo crear el producto.');
    }
    return created;
  }

  actualizar(id: ProductoId, input: UpdateProductoInput): Producto {
    const data = normalizeProductWrite(input);
    this.db.withTransactionSync(() => {
      this.assertCategoryExists(data.categoriaId);
      this.assertSkuAvailable(data.sku, id);
      this.db.runSync(
        `UPDATE products
            SET category_id = ?, sku = ?, name = ?, selling_price = ?, purchase_price = ?,
                current_stock = ?, min_stock = ?, image_uri = ?,
                updated_at = CURRENT_TIMESTAMP
          WHERE id = ? AND deleted_at IS NULL`,
        data.categoriaId,
        data.sku,
        data.nombre,
        data.precioVenta,
        data.precioCompra,
        data.stockActual,
        data.stockMinimo,
        data.imagenUri,
        id,
      );
    });
    const updated = this.findById(id);
    if (!updated) {
      throw new Error('No se pudo actualizar el producto.');
    }
    return updated;
  }

  eliminar(id: ProductoId): void {
    this.db.withTransactionSync(() => {
      this.db.runSync(
        'UPDATE products SET deleted_at = CURRENT_TIMESTAMP, updated_at = CURRENT_TIMESTAMP WHERE id = ? AND deleted_at IS NULL',
        id,
      );
    });
  }

  private assertCategoryExists(categoriaId: CategoriaId): void {
    const row = this.db.getFirstSync<{ id: number }>(
      'SELECT id FROM categories WHERE id = ? AND deleted_at IS NULL',
      categoriaId,
    );
    if (!row) {
      throw new Error('La categoría es obligatoria.');
    }
  }

  private assertSkuAvailable(sku: string | null, excludeId: ProductoId | null): void {
    if (!sku) return;
    const row = this.db.getFirstSync<{ id: number }>(
      `SELECT id FROM products
        WHERE sku = ? AND deleted_at IS NULL AND (? IS NULL OR id <> ?)`,
      sku,
      excludeId,
      excludeId,
    );
    if (row) {
      throw new Error('El SKU ya existe.');
    }
  }
}
