import type { ResumenCaja } from '@/domain/entities/ResumenCaja';
import { Money } from '@/domain/value-objects/Money';

export interface FinancialAccumulatorRowShape {
  readonly totalVentas: number;
  readonly totalCompras: number;
  readonly totalGastos: number;
  readonly saldoActual: number;
}

export function hydrateFinancialSummary(row: FinancialAccumulatorRowShape): ResumenCaja {
  return {
    totalVentas: new Money(row.totalVentas, 'Bs').amount,
    totalCompras: new Money(row.totalCompras, 'Bs').amount,
    totalGastos: new Money(row.totalGastos, 'Bs').amount,
    saldoActual: new Money(row.saldoActual, 'Bs').amount,
  };
}

export interface AccumulatorDelta {
  readonly salesDelta?: number;
  readonly purchasesDelta?: number;
  readonly expensesDelta?: number;
}

export function normalizeAccumulatorDelta(delta: AccumulatorDelta): {
  readonly salesDelta: number;
  readonly purchasesDelta: number;
  readonly expensesDelta: number;
  readonly netDelta: number;
} {
  const salesDelta = new Money(delta.salesDelta ?? 0, 'Bs').amount;
  const purchasesDelta = new Money(delta.purchasesDelta ?? 0, 'Bs').amount;
  const expensesDelta = new Money(delta.expensesDelta ?? 0, 'Bs').amount;
  const netDelta = new Money(salesDelta - purchasesDelta - expensesDelta, 'Bs').amount;
  return { salesDelta, purchasesDelta, expensesDelta, netDelta };
}
