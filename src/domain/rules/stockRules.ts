import type { Stock } from '../value-objects/Stock';

export type StockErrorReason = 'NON_POSITIVE_QUANTITY' | 'INSUFFICIENT_STOCK';

export interface StockProduct {
  readonly id: number;
  readonly name: string;
  readonly currentStock: Stock;
}

export interface StockErrorParams {
  readonly reason: StockErrorReason;
  readonly productId: number;
  readonly productName: string;
  readonly available: Stock;
  readonly requested: Stock;
}

export class StockError extends Error {
  readonly reason: StockErrorReason;
  readonly productId: number;
  readonly productName: string;
  readonly available: Stock;
  readonly requested: Stock;

  constructor(params: StockErrorParams) {
    super(buildMessage(params));
    this.name = 'StockError';
    this.reason = params.reason;
    this.productId = params.productId;
    this.productName = params.productName;
    this.available = params.available;
    this.requested = params.requested;
  }
}

export function assertStockAvailable(product: StockProduct, requested: Stock): void {
  if (requested.value <= 0) {
    throw new StockError({
      reason: 'NON_POSITIVE_QUANTITY',
      productId: product.id,
      productName: product.name,
      available: product.currentStock,
      requested,
    });
  }

  if (requested.value > product.currentStock.value) {
    throw new StockError({
      reason: 'INSUFFICIENT_STOCK',
      productId: product.id,
      productName: product.name,
      available: product.currentStock,
      requested,
    });
  }
}

export function isBelowMin(currentStock: Stock, minStock: Stock): boolean {
  return currentStock.isBelow(minStock);
}

function buildMessage(params: StockErrorParams): string {
  if (params.reason === 'NON_POSITIVE_QUANTITY') {
    return 'Requested quantity must be greater than zero';
  }

  return `Insufficient stock for "${params.productName}". Available: ${params.available.value}, requested: ${params.requested.value}.`;
}
