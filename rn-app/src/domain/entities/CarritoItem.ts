import type { Producto } from './Producto';

export type CarritoTipo = 'VENTA' | 'COMPRA';

export interface CarritoItem {
  readonly producto: Pick<
    Producto,
    'id' | 'nombre' | 'precioVenta' | 'precioCompra' | 'stockActual' | 'imagenUri'
  >;
  readonly cantidad: number;
  readonly descuento: number;
}
