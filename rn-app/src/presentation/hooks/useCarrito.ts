import type { CarritoTipo } from '@/domain/entities/CarritoItem';
import { calcTotals } from '@/domain/rules/cartRules';
import type { CurrencyCode } from '@/domain/value-objects/Currency';
import type { CarritoStoreHook } from '@/presentation/stores/carritoStore';
import { toCartKind, toCartLines } from '@/shared/utils/legacyCart';

export function useCarritoTotals(
  store: CarritoStoreHook,
  tipo: CarritoTipo,
  currency: CurrencyCode,
) {
  const items = store((s) => s.items);
  return calcTotals(toCartLines(items, toCartKind(tipo), currency), currency);
}

export function useCarritoCount(store: CarritoStoreHook) {
  return store((s) => s.items.length);
}
