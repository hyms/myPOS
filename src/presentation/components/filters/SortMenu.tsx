import React, { useState } from 'react';
import { Modal, Pressable, Text, View } from 'react-native';

import type { SortOrder } from '@/data/repositories/IProductoRepository';
import { cn } from '@/shared/utils/cn';

interface Props {
  readonly value: SortOrder;
  readonly onChange: (v: SortOrder) => void;
}

const OPTIONS: ReadonlyArray<{ value: SortOrder; label: string }> = [
  { value: 'POPULARIDAD_DESC', label: 'Más vendidos' },
  { value: 'NOMBRE_ASC', label: 'Nombre A–Z' },
];

export function SortMenu({ value, onChange }: Props) {
  const [open, setOpen] = useState(false);
  const current = OPTIONS.find((o) => o.value === value)?.label ?? 'Ordenar';

  return (
    <>
      <Pressable
        onPress={() => setOpen(true)}
        className="rounded-xl border border-surface-200 bg-white px-3 py-2 dark:border-surface-700 dark:bg-surface-900"
      >
        <Text className="text-sm text-surface-800 dark:text-surface-100">⇅ {current}</Text>
      </Pressable>
      <Modal transparent visible={open} animationType="fade" onRequestClose={() => setOpen(false)}>
        <Pressable onPress={() => setOpen(false)} className="flex-1 items-center justify-center bg-black/40 px-6">
          <View className="w-full max-w-xs rounded-2xl bg-white p-3 dark:bg-surface-900">
            {OPTIONS.map((opt) => (
              <Pressable
                key={opt.value}
                onPress={() => {
                  onChange(opt.value);
                  setOpen(false);
                }}
                className={cn(
                  'rounded-lg px-3 py-3 active:bg-surface-100',
                  opt.value === value && 'bg-primary-50',
                )}
              >
                <Text
                  className={cn(
                    'text-base',
                    opt.value === value ? 'font-bold text-primary-700' : 'text-surface-800',
                  )}
                >
                  {opt.label}
                </Text>
              </Pressable>
            ))}
          </View>
        </Pressable>
      </Modal>
    </>
  );
}
