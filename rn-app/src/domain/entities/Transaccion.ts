export type TipoTransaccion = 'VENTA' | 'COMPRA' | 'GASTO';
export type TipoPago = 'EFECTIVO' | 'TARJETA' | 'TRANSFERENCIA' | 'QR';

export interface Transaccion {
  readonly id: number;
  readonly tipo: TipoTransaccion;
  readonly montoTotal: number;
  readonly tipoPago: TipoPago;
  readonly detalle: string | null;
  readonly fechaRegistro: string;
  readonly fechaDelete: string | null;
}
