import type { DetalleTransaccion } from '@/domain/entities/DetalleTransaccion';
import { isPaymentMethodCode, type PaymentMethodCode } from '@/domain/entities/PaymentMethod';
import type { Transaccion } from '@/domain/entities/Transaccion';
import { isTransactionType, type TransactionType } from '@/domain/entities/TransactionType';
import { Money } from '@/domain/value-objects/Money';
import { Stock } from '@/domain/value-objects/Stock';

export interface TransactionRowShape {
  readonly id: number;
  readonly tipo: string;
  readonly montoTotal: number;
  readonly tipoPago: string;
  readonly detalle: string | null;
  readonly fechaRegistro: string;
  readonly fechaDelete: string | null;
}

export interface TransactionDetailRowShape {
  readonly id: number;
  readonly transaccionId: number;
  readonly productoId: number;
  readonly cantidad: number;
  readonly precioUnitario: number;
}

export function normalizeTransactionType(value: string): TransactionType {
  const upper = value.trim().toUpperCase();
  const map: Record<string, TransactionType> = {
    SALE: 'SALE',
    VENTA: 'SALE',
    PURCHASE: 'PURCHASE',
    COMPRA: 'PURCHASE',
    EXPENSE: 'EXPENSE',
    GASTO: 'EXPENSE',
  };
  const mapped = map[upper];
  if (mapped && isTransactionType(mapped)) return mapped;
  if (!isTransactionType(value)) {
    throw new Error(`Invalid transaction type: ${value}`);
  }
  return value;
}

export function normalizePaymentMethod(value: string): PaymentMethodCode {
  const upper = value.trim().toUpperCase();
  const map: Record<string, PaymentMethodCode> = {
    CASH: 'CASH',
    EFECTIVO: 'CASH',
    CARD: 'CARD',
    TARJETA: 'CARD',
    TRANSFER: 'TRANSFER',
    TRANSFERENCIA: 'TRANSFER',
    QR: 'QR',
  };
  const mapped = map[upper];
  if (mapped && isPaymentMethodCode(mapped)) return mapped;
  if (!isPaymentMethodCode(value)) {
    throw new Error(`Invalid payment method: ${value}`);
  }
  return value;
}

export function hydrateTransaction(row: TransactionRowShape): Transaccion {
  const tipo = normalizeTransactionType(row.tipo) as unknown as Transaccion['tipo'];
  const tipoPago = normalizePaymentMethod(row.tipoPago) as unknown as Transaccion['tipoPago'];
  // Keep legacy Spanish stored codes in mapped domain for compatibility with existing app/tests.
  const legacyTipo: Record<TransactionType, string> = { SALE: 'VENTA', PURCHASE: 'COMPRA', EXPENSE: 'GASTO' };
  const legacyPago: Record<PaymentMethodCode, string> = {
    CASH: 'EFECTIVO',
    CARD: 'TARJETA',
    TRANSFER: 'TRANSFERENCIA',
    QR: 'QR',
  };
  const montoTotal = new Money(row.montoTotal, 'Bs').amount;
  return {
    id: row.id,
    tipo: (legacyTipo[tipo as unknown as TransactionType] ?? row.tipo) as Transaccion['tipo'],
    montoTotal,
    tipoPago: (legacyPago[tipoPago as unknown as PaymentMethodCode] ?? row.tipoPago) as Transaccion['tipoPago'],
    detalle: row.detalle,
    fechaRegistro: row.fechaRegistro,
    fechaDelete: row.fechaDelete,
  };
}

export function hydrateTransactionDetail(row: TransactionDetailRowShape): DetalleTransaccion {
  const cantidad = new Stock(row.cantidad).value;
  const precioUnitario = new Money(row.precioUnitario, 'Bs').amount;
  return {
    id: row.id,
    transaccionId: row.transaccionId,
    productoId: row.productoId,
    cantidad,
    precioUnitario,
  };
}
