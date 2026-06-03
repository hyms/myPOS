import type { Producto } from '../entities/Producto';

export class StockError extends Error {
  constructor(
    message: string,
    public readonly productoId: number,
    public readonly productoNombre: string,
  ) {
    super(message);
    this.name = 'StockError';
  }
}

export function validateStockDisponible(
  producto: Pick<Producto, 'id' | 'nombre' | 'stockActual'>,
  cantidadSolicitada: number,
): void {
  if (cantidadSolicitada <= 0) {
    throw new StockError(
      `La cantidad debe ser mayor a 0.`,
      producto.id,
      producto.nombre,
    );
  }
  if (producto.stockActual - cantidadSolicitada < 0) {
    throw new StockError(
      `Stock insuficiente para "${producto.nombre}". Disponible: ${producto.stockActual}.`,
      producto.id,
      producto.nombre,
    );
  }
}

export function isBelowMin(
  producto: Pick<Producto, 'stockActual' | 'stockMinimo'>,
): boolean {
  return producto.stockActual < producto.stockMinimo;
}
