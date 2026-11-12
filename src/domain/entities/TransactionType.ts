export type TransactionType = 'SALE' | 'PURCHASE' | 'EXPENSE';

export const TRANSACTION_TYPES: ReadonlyArray<TransactionType> = Object.freeze([
  'SALE',
  'PURCHASE',
  'EXPENSE',
] as const);

const TRANSACTION_TYPE_SET: ReadonlySet<string> = new Set<string>(TRANSACTION_TYPES);

export function isTransactionType(value: string): value is TransactionType {
  return TRANSACTION_TYPE_SET.has(value);
}
