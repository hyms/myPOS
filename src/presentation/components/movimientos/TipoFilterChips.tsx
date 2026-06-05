import React, { memo, useCallback } from 'react';
import { Pressable, ScrollView, Text } from 'react-native';

import { cn } from '@/shared/utils/cn';

export type TipoFilter = 'ALL' | 'VENTA' | 'COMPRA' | 'GASTO';

interface Props {
  readonly value: TipoFilter;
  readonly onChange: (value: TipoFilter) => void;
}

const OPTIONS: ReadonlyArray<{ value: TipoFilter; label: string }> = [
  { value: 'ALL', label: 'Todas' },
  { value: 'VENTA', label: 'Ventas' },
  { value: 'COMPRA', label: 'Compras' },
  { value: 'GASTO', label: 'Gastos' },
];

function TipoFilterChipsComponent({ value, onChange }: Props) {
  return (
    <ScrollView
      horizontal
      showsHorizontalScrollIndicator={false}
      contentContainerClassName="gap-2 px-3 py-2"
    >
      {OPTIONS.map((opt) => (
        <Chip
          key={opt.value}
          label={opt.label}
          active={value === opt.value}
          onPress={() => onChange(opt.value)}
        />
      ))}
    </ScrollView>
  );
}

function Chip({ label, active, onPress }: { label: string; active: boolean; onPress: () => void }) {
  return (
    <Pressable
      onPress={onPress}
      accessibilityRole="button"
      accessibilityState={{ selected: active }}
      className={cn(
        'rounded-full border px-3 py-1.5',
        active
          ? 'border-surface-900 bg-surface-900 dark:border-surface-50 dark:bg-surface-50'
          : 'border-surface-300 bg-white dark:border-surface-700 dark:bg-surface-900',
      )}
    >
      <Text
        className={cn(
          'text-sm font-medium',
          active ? 'text-white dark:text-surface-900' : 'text-surface-700 dark:text-surface-200',
        )}
      >
        {label}
      </Text>
    </Pressable>
  );
}

export const TipoFilterChips = memo(TipoFilterChipsComponent);
