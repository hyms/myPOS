import React, { memo } from 'react';
import { Text, View } from 'react-native';

import type { CarritoTipo } from '@/domain/entities/CarritoItem';
import type { CarritoStoreHook } from '@/presentation/stores/carritoStore';
import { useCarritoTotals } from '@/presentation/hooks/useCarrito';
import { useCurrency } from '@/presentation/hooks/useCurrency';
import { AnimatedPressable } from '@/presentation/components/ui/AnimatedPressable';
import { Badge } from '@/presentation/components/ui/Card';

interface Props {
  readonly store: CarritoStoreHook;
  readonly tipo: CarritoTipo;
  readonly onCheckout: () => void;
}

function CartSummaryBarComponent({ store, tipo, onCheckout }: Props) {
  const count = store((s) => s.items.length);
  const totals = useCarritoTotals(store, tipo);
  const { format } = useCurrency();

  if (count === 0) return null;
  return (
    <View className="border-t border-surface-200 bg-white p-3 dark:border-surface-800 dark:bg-surface-900">
      <View className="mb-2 flex-row items-center justify-between">
        <Badge label={`${count} ítem${count === 1 ? '' : 's'}`} tone="primary" />
        <Text className="text-2xl font-bold text-primary-700">{format(totals.total)}</Text>
      </View>
      <AnimatedPressable
        onPress={onCheckout}
        className="rounded-xl bg-primary-600 px-4 py-3"
        accessibilityRole="button"
      >
        <Text className="text-center text-base font-bold text-white">Continuar al pago</Text>
      </AnimatedPressable>
    </View>
  );
}

export const CartSummaryBar = memo(CartSummaryBarComponent);
