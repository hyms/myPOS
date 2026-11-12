import type { Categoria } from '@/domain/entities/Categoria';
import type { DetalleTransaccion } from '@/domain/entities/DetalleTransaccion';
import type { Producto } from '@/domain/entities/Producto';
import type { ResumenCaja } from '@/domain/entities/ResumenCaja';
import type {
  TipoPago,
  TipoTransaccion,
  Transaccion,
} from '@/domain/entities/Transaccion';

export interface CategoriaRow {
  id: number;
  name: string;
  registered_at: string;
  deleted_at: string | null;
}

export interface ProductoRow {
  id: number;
  category_id: number;
  sku: string | null;
  name: string;
  selling_price: number;
  purchase_price: number;
  current_stock: number;
  min_stock: number;
  image_uri: string | null;
  registered_at: string;
  updated_at: string;
  deleted_at: string | null;
}

export interface TransaccionRow {
  id: number;
  type: string;
  total_amount: number;
  payment_method: string;
  detail: string | null;
  registered_at: string;
  deleted_at: string | null;
}

export interface DetalleTransaccionRow {
  id: number;
  transaction_id: number;
  product_id: number;
  quantity: number;
  unit_price: number;
}

export interface ResumenCajaRow {
  total_sales: number;
  total_purchases: number;
  total_expenses: number;
  net_balance: number;
}

export interface ProductoPopularidadRow extends ProductoRow {
  popularidad: number;
}

export function toCategoria(row: CategoriaRow): Categoria {
  return {
    id: row.id,
    nombre: row.name,
    fechaRegistro: row.registered_at,
    fechaDelete: row.deleted_at,
  };
}

export function toProducto(row: ProductoRow): Producto {
  return {
    id: row.id,
    categoriaId: row.category_id,
    nombre: row.name,
    precioVenta: row.selling_price,
    precioCompra: row.purchase_price,
    stockActual: row.current_stock,
    stockMinimo: row.min_stock,
    imagenUri: row.image_uri,
    fechaRegistro: row.registered_at,
    fechaUpdate: row.updated_at,
    fechaDelete: row.deleted_at,
  };
}

export function toTransaccion(row: TransaccionRow): Transaccion {
  return {
    id: row.id,
    tipo: row.type as TipoTransaccion,
    montoTotal: row.total_amount,
    tipoPago: row.payment_method as TipoPago,
    detalle: row.detail,
    fechaRegistro: row.registered_at,
    fechaDelete: row.deleted_at,
  };
}

export function toDetalleTransaccion(row: DetalleTransaccionRow): DetalleTransaccion {
  return {
    id: row.id,
    transaccionId: row.transaction_id,
    productoId: row.product_id,
    cantidad: row.quantity,
    precioUnitario: row.unit_price,
  };
}

export function toResumenCaja(row: ResumenCajaRow): ResumenCaja {
  return {
    totalVentas: row.total_sales,
    totalCompras: row.total_purchases,
    totalGastos: row.total_expenses,
    saldoActual: row.net_balance,
  };
}
