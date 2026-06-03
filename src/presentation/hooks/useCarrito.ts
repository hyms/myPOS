import { useCarritoStore } from '@/presentation/stores/carritoStore';
import { calcTotals } from '@/domain/rules/cartRules';

export function useCarritoTotals() {
  const items = useCarritoStore((s) => s.items);
  const tipo = useCarritoStore((s) => s.tipo);
  return calcTotals(items, tipo);
}

export function useCarritoCount() {
  return useCarritoStore((s) => s.items.length);
}
