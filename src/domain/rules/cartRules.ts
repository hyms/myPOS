import type { CarritoItem, CarritoTipo } from '../entities/CarritoItem';

export interface CartTotals {
  readonly subtotal: number;
  readonly descuento: number;
  readonly total: number;
}

export function getUnitPrice(item: CarritoItem, tipo: CarritoTipo): number {
  return tipo === 'VENTA' ? item.producto.precioVenta : item.producto.precioCompra;
}

export function lineSubtotal(item: CarritoItem, tipo: CarritoTipo): number {
  const unitPrice = getUnitPrice(item, tipo);
  return unitPrice * item.cantidad;
}

export function calcTotals(items: ReadonlyArray<CarritoItem>, tipo: CarritoTipo): CartTotals {
  let subtotal = 0;
  let descuento = 0;
  for (const item of items) {
    subtotal += lineSubtotal(item, tipo);
    descuento += item.descuento;
  }
  return {
    subtotal: round2(subtotal),
    descuento: round2(descuento),
    total: round2(subtotal - descuento),
  };
}

export function calcChange(total: number, recibido: number): number {
  return round2(recibido - total);
}

export function applyDiscount(
  subtotal: number,
  descuento: number,
): number {
  if (descuento < 0) return subtotal;
  if (descuento > subtotal) return 0;
  return round2(subtotal - descuento);
}

function round2(value: number): number {
  return Math.round(value * 100) / 100;
}
