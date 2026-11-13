export interface DetalleTransaccion {
  readonly id: number;
  readonly transaccionId: number;
  readonly productoId: number;
  readonly cantidad: number;
  readonly precioUnitario: number;
}
