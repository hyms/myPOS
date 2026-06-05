import React, { memo } from 'react';
import { Pressable, Text, View } from 'react-native';

import { cn } from '@/shared/utils/cn';
import type { CarritoTipo } from '@/domain/entities/CarritoItem';

interface Props {
  readonly value: CarritoTipo;
  readonly onChange: (next: CarritoTipo) => void;
}

const OPTIONS: ReadonlyArray<{ value: CarritoTipo; label: string; tone: 'success' | 'warning' }> = [
  { value: 'VENTA', label: 'Venta', tone: 'success' },
  { value: 'COMPRA', label: 'Compra', tone: 'warning' },
];

function RegistroSegmentedComponent({ value, onChange }: Props) {
  return (
    <View className="m-3 flex-row rounded-full border border-border-subtle bg-surface-lo p-1">
      {OPTIONS.map((opt) => {
        const active = opt.value === value;
        return (
          <Pressable
            key={opt.value}
            onPress={() => onChange(opt.value)}
            accessibilityRole="tab"
            accessibilityState={{ selected: active }}
            accessibilityLabel={opt.label}
            className={cn(
              'flex-1 items-center justify-center rounded-full py-2.5',
              active
                ? opt.tone === 'success'
                  ? 'bg-success'
                  : 'bg-warning'
                : 'bg-transparent',
            )}
          >
            <Text
              className={cn(
                'text-sm font-bold',
                active
                  ? opt.tone === 'success'
                    ? 'text-onSuccess'
                    : 'text-onWarning'
                  : 'text-ink',
              )}
            >
              {opt.label}
            </Text>
          </Pressable>
        );
      })}
    </View>
  );
}

export const RegistroSegmented = memo(RegistroSegmentedComponent);
