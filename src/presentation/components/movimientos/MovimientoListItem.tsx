import React, { memo } from 'react';
import { Text, View } from 'react-native';

import { AnimatedPressable } from '@/presentation/components/ui/AnimatedPressable';
import type { Transaccion, TipoTransaccion } from '@/domain/entities/Transaccion';
import { formatFechaCorta } from '@/shared/utils/date';
import { cn } from '@/shared/utils/cn';

export type MovimientoTone = 'success' | 'warning' | 'danger';

const TONE: Readonly<Record<MovimientoTone, { bar: string; bg: string; text: string; chip: string; sign: string }>> = {
  success: { bar: 'bg-success-500', bg: 'bg-success-50', text: 'text-success-800', chip: 'text-success-700', sign: '+' },
  warning: { bar: 'bg-warning-500', bg: 'bg-warning-50', text: 'text-warning-800', chip: 'text-warning-700', sign: '-' },
  danger:  { bar: 'bg-danger-500',  bg: 'bg-danger-50',  text: 'text-danger-800',  chip: 'text-danger-700',  sign: '-' },
};

const TIPO_TONE: Readonly<Record<TipoTransaccion, MovimientoTone>> = {
  VENTA: 'success',
  COMPRA: 'warning',
  GASTO: 'danger',
};

const TIPO_LABEL: Readonly<Record<TipoTransaccion, string>> = {
  VENTA: 'Venta',
  COMPRA: 'Compra',
  GASTO: 'Gasto',
};

interface Props {
  readonly item: Transaccion;
  readonly detailCount?: number;
  readonly onPress: (id: number) => void;
  readonly format: (amount: number) => string;
}

function MovimientoListItemComponent({ item, detailCount, onPress, format }: Props) {
  const tone = TONE[TIPO_TONE[item.tipo]];
  return (
    <AnimatedPressable
      onPress={() => onPress(item.id)}
      accessibilityRole="button"
      accessibilityLabel={`${TIPO_LABEL[item.tipo]} de ${format(item.montoTotal)}`}
      className="mx-3 my-1.5 overflow-hidden rounded-2xl border border-surface-100 bg-white dark:border-surface-800 dark:bg-surface-900"
    >
      <View className={cn('h-1 w-full', tone.bar)} />
      <View className="flex-row items-center justify-between p-3.5">
        <View className="flex-1 pr-3">
          <View className="flex-row items-center gap-2">
            <Text className={cn('text-xs font-semibold uppercase tracking-wide', tone.chip)}>
              {TIPO_LABEL[item.tipo]}
            </Text>
            <Text className="text-[11px] text-surface-500">{item.tipoPago}</Text>
          </View>
          <Text
            className="mt-0.5 text-sm font-semibold text-surface-900 dark:text-surface-50"
            numberOfLines={2}
          >
            {item.detalle ?? (detailCount && detailCount > 0 ? `${detailCount} ${detailCount === 1 ? 'ítem' : 'ítems'}` : '—')}
          </Text>
          <Text className="mt-0.5 text-xs text-surface-500">
            {formatFechaCorta(item.fechaRegistro)}
          </Text>
        </View>
        <View className="items-end">
          <Text className={cn('text-lg font-extrabold', tone.text)}>
            {tone.sign}{format(Math.abs(item.montoTotal))}
          </Text>
        </View>
      </View>
    </AnimatedPressable>
  );
}

export const MovimientoListItem = memo(MovimientoListItemComponent);
