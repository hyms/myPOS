import type { StockError } from '@/domain/rules/stockRules';

export function stockErrorMessage(error: StockError): string {
  if (error.reason === 'NON_POSITIVE_QUANTITY') {
    return 'La cantidad debe ser mayor a 0.';
  }

  return `Stock insuficiente para "${error.productName}". Disponible: ${error.available.value}.`;
}
