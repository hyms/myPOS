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
  nombre: string;
  fecha_registro: string;
  fecha_delete: string | null;
}

export interface ProductoRow {
  id: number;
  categoria_id: number;
  nombre: string;
  precio_venta: number;
  precio_compra: number;
  stock_actual: number;
  stock_minimo: number;
  imagen_uri: string | null;
  fecha_registro: string;
  fecha_update: string;
  fecha_delete: string | null;
}

export interface TransaccionRow {
  id: number;
  tipo: string;
  monto_total: number;
  tipo_pago: string;
  detalle: string | null;
  fecha_registro: string;
  fecha_delete: string | null;
}

export interface DetalleTransaccionRow {
  id: number;
  transaccion_id: number;
  producto_id: number;
  cantidad: number;
  precio_unitario: number;
}

export interface ResumenCajaRow {
  total_ventas: number;
  total_compras: number;
  total_gastos: number;
  saldo_actual: number;
}

export interface ProductoPopularidadRow extends ProductoRow {
  popularidad: number;
}

export function toCategoria(row: CategoriaRow): Categoria {
  return {
    id: row.id,
    nombre: row.nombre,
    fechaRegistro: row.fecha_registro,
    fechaDelete: row.fecha_delete,
  };
}

export function toProducto(row: ProductoRow): Producto {
  return {
    id: row.id,
    categoriaId: row.categoria_id,
    nombre: row.nombre,
    precioVenta: row.precio_venta,
    precioCompra: row.precio_compra,
    stockActual: row.stock_actual,
    stockMinimo: row.stock_minimo,
    imagenUri: row.imagen_uri,
    fechaRegistro: row.fecha_registro,
    fechaUpdate: row.fecha_update,
    fechaDelete: row.fecha_delete,
  };
}

export function toTransaccion(row: TransaccionRow): Transaccion {
  return {
    id: row.id,
    tipo: row.tipo as TipoTransaccion,
    montoTotal: row.monto_total,
    tipoPago: row.tipo_pago as TipoPago,
    detalle: row.detalle,
    fechaRegistro: row.fecha_registro,
    fechaDelete: row.fecha_delete,
  };
}

export function toDetalleTransaccion(row: DetalleTransaccionRow): DetalleTransaccion {
  return {
    id: row.id,
    transaccionId: row.transaccion_id,
    productoId: row.producto_id,
    cantidad: row.cantidad,
    precioUnitario: row.precio_unitario,
  };
}

export function toResumenCaja(row: ResumenCajaRow): ResumenCaja {
  return {
    totalVentas: row.total_ventas,
    totalCompras: row.total_compras,
    totalGastos: row.total_gastos,
    saldoActual: row.saldo_actual,
  };
}
