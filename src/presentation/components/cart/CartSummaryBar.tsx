import React, { memo } from 'react';
import { Text, View } from 'react-native';

import type { CarritoTipo } from '@/domain/entities/CarritoItem';
import type { CarritoStoreHook } from '@/presentation/stores/carritoStore';
import { useCarritoTotals } from '@/presentation/hooks/useCarrito';
import { useCurrency } from '@/presentation/hooks/useCurrency';
import { AnimatedPressable } from '@/presentation/components/ui/AnimatedPressable';
import { Badge } from '@/presentation/components/ui/Card';
import { Icon } from '@/presentation/components/ui/Icon';

interface Props {
  readonly store: CarritoStoreHook;
  readonly tipo: CarritoTipo;
  readonly onCheckout: () => void;
  readonly onClear: () => void;
}

function CartSummaryBarComponent({ store, tipo, onCheckout, onClear }: Props) {
  const count = store((s) => s.items.length);
  const totals = useCarritoTotals(store, tipo);
  const { format } = useCurrency();

  if (count === 0) return null;
  return (
    <View
      accessibilityLabel="Resumen y acciones del carrito"
      className="border-t border-surface-200 bg-white px-3 pb-3 pt-2 dark:border-surface-800 dark:bg-surface-900"
    >
      <View className="mb-2 flex-row items-center justify-between">
        <View className="flex-row items-center gap-2">
          <Badge label={`${count} ítem${count === 1 ? '' : 's'}`} tone="primary" />
          <AnimatedPressable
            onPress={onClear}
            accessibilityRole="button"
            accessibilityLabel="Vaciar carrito"
            hitSlop={8}
            scaleTo={0.95}
            opacityTo={0.6}
            className="rounded-full bg-danger-50 px-2.5 py-1 dark:bg-danger-950"
          >
            <Text className="text-[11px] font-bold uppercase tracking-wide text-danger-600">
              Vaciar
            </Text>
          </AnimatedPressable>
        </View>
        <Text className="text-2xl font-bold tabular-nums text-primary-700 dark:text-primary-300">
          {format(totals.total)}
        </Text>
      </View>
      <AnimatedPressable
        onPress={onCheckout}
        scaleTo={0.98}
        opacityTo={0.9}
        accessibilityRole="button"
        accessibilityLabel={tipo === 'VENTA' ? 'Continuar al cobro' : 'Continuar al pago'}
        className="min-h-[48px] flex-row items-center justify-center gap-2 rounded-xl bg-primary-600 px-4 py-3 active:bg-primary-700"
      >
        <Text className="text-base font-bold text-white">
          {tipo === 'VENTA' ? 'Cobrar venta' : 'Registrar compra'}
        </Text>
        <Icon name="arrow-forward" size={18} color="#ffffff" />
      </AnimatedPressable>
    </View>
  );
}

export const CartSummaryBar = memo(CartSummaryBarComponent);
