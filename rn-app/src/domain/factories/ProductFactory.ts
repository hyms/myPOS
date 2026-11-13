import type {
  CreateProductoInput,
  Producto,
  ProductoId,
  UpdateProductoInput,
} from '@/domain/entities/Producto';
import { SKU } from '@/domain/value-objects/SKU';
import { Stock } from '@/domain/value-objects/Stock';

export interface ProductRowShape {
  readonly id: number;
  readonly categoriaId: number;
  readonly sku: string | null;
  readonly nombre: string;
  readonly precioVenta: number;
  readonly precioCompra: number;
  readonly stockActual: number;
  readonly stockMinimo: number;
  readonly imagenUri: string | null;
  readonly fechaRegistro: string;
  readonly fechaUpdate: string;
  readonly fechaDelete: string | null;
}

export interface NormalizedProductWrite {
  readonly categoriaId: number;
  readonly sku: string | null;
  readonly nombre: string;
  readonly precioVenta: number;
  readonly precioCompra: number;
  readonly stockActual: number;
  readonly stockMinimo: number;
  readonly imagenUri: string | null;
}

function normalizeName(name: string): string {
  const trimmed = name.trim();
  if (trimmed.length === 0) {
    throw new Error('El nombre del producto es obligatorio.');
  }
  return trimmed;
}

export function normalizeSku(sku: string | null | undefined): string | null {
  if (sku === undefined || sku === null) return null;
  const trimmed = sku.trim();
  if (trimmed.length === 0) return null;
  return new SKU(trimmed).value;
}

export function normalizeProductWrite(
  input: CreateProductoInput | UpdateProductoInput,
): NormalizedProductWrite {
  if (input.categoriaId === null || input.categoriaId === undefined) {
    throw new Error('La categoría es obligatoria.');
  }
  if (input.precioVenta < 0 || input.precioCompra < 0) {
    throw new Error('Los precios no pueden ser negativos.');
  }
  if (input.stockActual < 0 || input.stockMinimo < 0) {
    throw new Error('El stock no puede ser negativo.');
  }
  const stockActual = new Stock(input.stockActual).value;
  const stockMinimo = new Stock(input.stockMinimo).value;
  const sku = 'sku' in input ? normalizeSku(input.sku) : null;
  return {
    categoriaId: input.categoriaId,
    sku,
    nombre: normalizeName(input.nombre),
    precioVenta: input.precioVenta,
    precioCompra: input.precioCompra,
    stockActual,
    stockMinimo,
    imagenUri: input.imagenUri,
  };
}

export function hydrateProduct(row: ProductRowShape): Producto & { readonly sku: string | null } {
  const sku = row.sku === null ? null : new SKU(row.sku).value;
  const stockActual = new Stock(row.stockActual).value;
  const stockMinimo = new Stock(row.stockMinimo).value;
  return {
    id: row.id as ProductoId,
    categoriaId: row.categoriaId,
    sku,
    nombre: row.nombre,
    precioVenta: row.precioVenta,
    precioCompra: row.precioCompra,
    stockActual,
    stockMinimo,
    imagenUri: row.imagenUri,
    fechaRegistro: row.fechaRegistro,
    fechaUpdate: row.fechaUpdate,
    fechaDelete: row.fechaDelete,
  };
}
