import React, { useState } from 'react';
import { Modal, Pressable, Text, View } from 'react-native';

import type { SortOrder } from '@/data/repositories/IProductoRepository';
import { Icon } from '@/presentation/components/ui/Icon';
import { cn } from '@/shared/utils/cn';

interface Props {
  readonly value: SortOrder;
  readonly onChange: (v: SortOrder) => void;
}

const OPTIONS: ReadonlyArray<{ value: SortOrder; label: string; icon: 'trending-up' | 'text' }> = [
  { value: 'POPULARIDAD_DESC', label: 'Más vendidos', icon: 'trending-up' },
  { value: 'NOMBRE_ASC', label: 'Nombre A–Z', icon: 'text' },
];

export function SortMenu({ value, onChange }: Props) {
  const [open, setOpen] = useState(false);
  const current = OPTIONS.find((o) => o.value === value)?.label ?? 'Ordenar';

  return (
    <>
      <Pressable
        onPress={() => setOpen(true)}
        accessibilityRole="button"
        accessibilityLabel={`Ordenar productos, actual: ${current}`}
        hitSlop={8}
        className="min-h-[44px] flex-row items-center gap-2 rounded-xl border border-surface-200 bg-white px-3 dark:border-surface-700 dark:bg-surface-900"
      >
        <Icon name="swap-vertical" size={16} color="#64748b" />
        <Text className="text-sm font-semibold text-surface-800 dark:text-surface-100" numberOfLines={1}>
          {current}
        </Text>
      </Pressable>
      <Modal
        transparent
        visible={open}
        animationType="fade"
        onRequestClose={() => setOpen(false)}
        accessibilityViewIsModal
      >
        <Pressable
          onPress={() => setOpen(false)}
          accessibilityLabel="Cerrar menú"
          accessibilityRole="button"
          className="flex-1 items-center justify-center bg-black/55 px-6"
        >
          <View
            accessibilityRole="radiogroup"
            className="w-full max-w-xs gap-1 rounded-2xl bg-white p-2 shadow-2xl dark:bg-surface-900"
          >
            {OPTIONS.map((opt) => {
              const active = opt.value === value;
              return (
                <Pressable
                  key={opt.value}
                  onPress={() => {
                    onChange(opt.value);
                    setOpen(false);
                  }}
                  accessibilityRole="radio"
                  accessibilityState={{ selected: active }}
                  hitSlop={4}
                  className={cn(
                    'min-h-[44px] flex-row items-center gap-3 rounded-xl px-3 active:bg-surface-100 dark:active:bg-surface-800',
                    active && 'bg-primary-50 dark:bg-primary-950',
                  )}
                >
                  <Icon name={opt.icon} size={18} color={active ? '#b45309' : '#64748b'} />
                  <Text
                    className={cn(
                      'flex-1 text-base',
                      active ? 'font-bold text-primary-700 dark:text-primary-300' : 'text-surface-800 dark:text-surface-100',
                    )}
                  >
                    {opt.label}
                  </Text>
                  {active ? <Icon name="checkmark" size={18} color="#b45309" /> : null}
                </Pressable>
              );
            })}
          </View>
        </Pressable>
      </Modal>
    </>
  );
}
