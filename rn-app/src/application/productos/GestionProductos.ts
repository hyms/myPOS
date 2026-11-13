import { getRepositories } from '@/data/repositories/container';
import type {
  CreateProductoInput,
  Producto,
  ProductoId,
  UpdateProductoInput,
} from '@/domain/entities/Producto';

export function crearProducto(input: CreateProductoInput): Producto {
  return getRepositories().productos.crear(input);
}

export function actualizarProducto(id: ProductoId, input: UpdateProductoInput): Producto {
  return getRepositories().productos.actualizar(id, input);
}

export function eliminarProducto(id: ProductoId): void {
  getRepositories().productos.eliminar(id);
}
