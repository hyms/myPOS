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
import { toProducto, type ProductoPopularidadRow, type ProductoRow } from '@/data/mappers/rowMappers';
import type { IProductoRepository, ProductoQuery } from './IProductoRepository';

export class SqliteProductoRepository implements IProductoRepository {
  constructor(private readonly db: SQLiteDatabase) {}

  listar(query: ProductoQuery): ReadonlyArray<Producto & { popularidad: number }> {
    const searchLike = `%${(query.search ?? '').trim()}%`;
    const hasCategoria = query.categoriaId !== undefined && query.categoriaId !== null;
    const orderClause = query.sort === 'POPULARIDAD_DESC'
      ? 'popularidad DESC, p.nombre ASC'
      : 'p.nombre ASC';

    const rows = this.db.getAllSync<ProductoPopularidadRow>(
      `SELECT p.id, p.categoria_id, p.nombre, p.precio_venta, p.precio_compra,
              p.stock_actual, p.stock_minimo, p.imagen_uri, p.fecha_registro,
              p.fecha_update, p.fecha_delete,
              COALESCE(SUM(d.cantidad), 0) AS popularidad
         FROM productos p
         LEFT JOIN detalle_transacciones d ON d.producto_id = p.id
         LEFT JOIN transacciones t
           ON t.id = d.transaccion_id
          AND t.tipo = 'VENTA'
          AND t.fecha_delete IS NULL
        WHERE p.fecha_delete IS NULL
          AND (? IS NULL OR p.categoria_id = ?)
          AND p.nombre LIKE ?
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
      'SELECT id, categoria_id, nombre, precio_venta, precio_compra, stock_actual, stock_minimo, imagen_uri, fecha_registro, fecha_update, fecha_delete FROM productos WHERE id = ? AND fecha_delete IS NULL',
      id,
    );
    return row ? toProducto(row) : null;
  }

  findByIds(ids: ReadonlyArray<ProductoId>): ReadonlyArray<Producto> {
    if (ids.length === 0) return [];
    const placeholders = ids.map(() => '?').join(',');
    const rows = this.db.getAllSync<ProductoRow>(
      `SELECT id, categoria_id, nombre, precio_venta, precio_compra, stock_actual, stock_minimo, imagen_uri, fecha_registro, fecha_update, fecha_delete
         FROM productos
        WHERE id IN (${placeholders}) AND fecha_delete IS NULL`,
      ...ids,
    );
    return rows.map(toProducto);
  }

  crear(input: CreateProductoInput): Producto {
    this.validate(input.categoriaId, input);
    const result = this.db.runSync(
      `INSERT INTO productos
        (categoria_id, nombre, precio_venta, precio_compra, stock_actual, stock_minimo, imagen_uri)
       VALUES (?, ?, ?, ?, ?, ?, ?)`,
      input.categoriaId,
      input.nombre.trim(),
      input.precioVenta,
      input.precioCompra,
      input.stockActual,
      input.stockMinimo,
      input.imagenUri,
    );
    const created = this.findById(result.lastInsertRowId);
    if (!created) {
      throw new Error('No se pudo crear el producto.');
    }
    return created;
  }

  actualizar(id: ProductoId, input: UpdateProductoInput): Producto {
    this.validate(input.categoriaId, input);
    this.db.runSync(
      `UPDATE productos
          SET categoria_id = ?, nombre = ?, precio_venta = ?, precio_compra = ?,
              stock_actual = ?, stock_minimo = ?, imagen_uri = ?,
              fecha_update = CURRENT_TIMESTAMP
        WHERE id = ? AND fecha_delete IS NULL`,
      input.categoriaId,
      input.nombre.trim(),
      input.precioVenta,
      input.precioCompra,
      input.stockActual,
      input.stockMinimo,
      input.imagenUri,
      id,
    );
    const updated = this.findById(id);
    if (!updated) {
      throw new Error('No se pudo actualizar el producto.');
    }
    return updated;
  }

  eliminar(id: ProductoId): void {
    this.db.runSync(
      'UPDATE productos SET fecha_delete = CURRENT_TIMESTAMP, fecha_update = CURRENT_TIMESTAMP WHERE id = ?',
      id,
    );
  }

  private validate(categoriaId: CategoriaId, input: CreateProductoInput | UpdateProductoInput): void {
    if (!categoriaId) {
      throw new Error('La categoría es obligatoria.');
    }
    if (!input.nombre.trim()) {
      throw new Error('El nombre del producto es obligatorio.');
    }
    if (input.precioVenta < 0 || input.precioCompra < 0) {
      throw new Error('Los precios no pueden ser negativos.');
    }
    if (input.stockActual < 0 || input.stockMinimo < 0) {
      throw new Error('El stock no puede ser negativo.');
    }
  }
}
