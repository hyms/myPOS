import React, { memo } from 'react';
import { Text, View } from 'react-native';

import type { CarritoTipo } from '@/domain/entities/CarritoItem';
import { useCurrency } from '@/presentation/hooks/useCurrency';
import { AnimatedPressable } from '@/presentation/components/ui/AnimatedPressable';
import { Badge } from '@/presentation/components/ui/Card';
import { Icon } from '@/presentation/components/ui/Icon';
import { DARK_PALETTE } from '@/presentation/theme/tokens';

interface Props {
  readonly count: number;
  readonly total: number;
  readonly tipo: CarritoTipo;
  readonly onCheckout: () => void;
  readonly onClear: () => void;
}

function CartSummaryBarComponent({ count, total, tipo, onCheckout, onClear }: Props) {
  const { format } = useCurrency();

  if (count === 0) return null;
  return (
    <View
      accessibilityLabel="Resumen y acciones del carrito"
      className="border-t border-border-subtle bg-surface px-3 pb-3 pt-2"
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
            className="min-h-[44px] items-center justify-center rounded-full bg-danger-soft px-3"
          >
            <Text className="text-[11px] font-bold uppercase tracking-wide text-danger">
              Vaciar
            </Text>
          </AnimatedPressable>
        </View>
        <Text
          className="text-2xl font-bold tabular-nums text-accent-bright"
          selectable
        >
          {format(total)}
        </Text>
      </View>
      <AnimatedPressable
        onPress={onCheckout}
        scaleTo={0.98}
        opacityTo={0.9}
        accessibilityRole="button"
        accessibilityLabel={tipo === 'VENTA' ? 'Continuar al cobro' : 'Continuar al pago'}
        className="min-h-[48px] flex-row items-center justify-center gap-2 rounded-xl bg-accent px-4 py-3"
      >
        <Text className="text-base font-bold text-onAccent">
          {tipo === 'VENTA' ? 'Cobrar venta' : 'Registrar compra'}
        </Text>
        <Icon name="arrow-forward" size={18} color={DARK_PALETTE.inkStrong} />
      </AnimatedPressable>
    </View>
  );
}

export const CartSummaryBar = memo(CartSummaryBarComponent);
