import type { CarritoTipo } from '@/domain/entities/CarritoItem';
import { calcTotals } from '@/domain/rules/cartRules';
import type { CarritoStoreHook } from '@/presentation/stores/carritoStore';

export function useCarritoTotals(store: CarritoStoreHook, tipo: CarritoTipo) {
  const items = store((s) => s.items);
  return calcTotals(items, tipo);
}

export function useCarritoCount(store: CarritoStoreHook) {
  return store((s) => s.items.length);
}
