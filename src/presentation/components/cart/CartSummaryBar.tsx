import React, { memo } from 'react';
import { Pressable, Text, View } from 'react-native';

import { useCarritoStore } from '@/presentation/stores/carritoStore';
import { useCarritoTotals } from '@/presentation/hooks/useCarrito';
import { useCurrency } from '@/presentation/hooks/useCurrency';
import { Badge } from '@/presentation/components/ui/Card';

interface Props {
  readonly onCheckout: () => void;
}

function CartSummaryBarComponent({ onCheckout }: Props) {
  const count = useCarritoStore((s) => s.items.length);
  const totals = useCarritoTotals();
  const { format } = useCurrency();

  if (count === 0) return null;
  return (
    <View className="border-t border-surface-200 bg-white p-3 dark:border-surface-800 dark:bg-surface-900">
      <View className="mb-2 flex-row items-center justify-between">
        <Badge label={`${count} ítem${count === 1 ? '' : 's'}`} tone="primary" />
        <Text className="text-2xl font-bold text-primary-700">{format(totals.total)}</Text>
      </View>
      <Pressable
        onPress={onCheckout}
        className="rounded-xl bg-primary-600 px-4 py-3 active:bg-primary-700"
        accessibilityRole="button"
      >
        <Text className="text-center text-base font-bold text-white">Continuar al pago</Text>
      </Pressable>
    </View>
  );
}

export const CartSummaryBar = memo(CartSummaryBarComponent);
