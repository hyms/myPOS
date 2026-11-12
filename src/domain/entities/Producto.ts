import type { CategoriaId } from './Categoria';

export type ProductoId = number;

export interface Producto {
  readonly id: ProductoId;
  readonly categoriaId: CategoriaId;
  readonly sku?: string | null;
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

export type ProductoResumen = Pick<
  Producto,
  'id' | 'nombre' | 'precioVenta' | 'precioCompra' | 'stockActual' | 'imagenUri' | 'categoriaId'
> & { readonly sku?: string | null };


export type CreateProductoInput = {
  readonly categoriaId: CategoriaId;
  readonly sku?: string | null;
  readonly nombre: string;
  readonly precioVenta: number;
  readonly precioCompra: number;
  readonly stockActual: number;
  readonly stockMinimo: number;
  readonly imagenUri: string | null;
};

export type UpdateProductoInput = {
  readonly categoriaId: CategoriaId;
  readonly sku?: string | null;
  readonly nombre: string;
  readonly precioVenta: number;
  readonly precioCompra: number;
  readonly stockActual: number;
  readonly stockMinimo: number;
  readonly imagenUri: string | null;
};
