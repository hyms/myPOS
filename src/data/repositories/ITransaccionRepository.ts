import type {
  DetalleTransaccion,
} from '@/domain/entities/DetalleTransaccion';
import type { TipoPago, TipoTransaccion, Transaccion } from '@/domain/entities/Transaccion';

export interface CreateTransaccionInput {
  readonly tipo: TipoTransaccion;
  readonly montoTotal: number;
  readonly tipoPago: TipoPago;
  readonly detalle: string | null;
}

export interface CreateDetalleInput {
  readonly productoId: number;
  readonly cantidad: number;
  readonly precioUnitario: number;
}

export interface ITransaccionRepository {
  findById(id: number): Transaccion | null;
  findDetalles(id: number): ReadonlyArray<DetalleTransaccion>;
  listar(options: {
    readonly tipo?: TipoTransaccion;
    readonly limit: number;
    readonly offset: number;
  }): ReadonlyArray<Transaccion>;
  listarPorMesAnio(options: {
    readonly periodo: 'MONTH' | 'YEAR';
  }): ReadonlyArray<{ tipo: TipoTransaccion; total: number }>;
  gastosDelMes(limit: number, offset: number): ReadonlyArray<Transaccion>;
}
