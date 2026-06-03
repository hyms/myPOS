import React, { useCallback } from 'react';
import { Pressable, Text, View } from 'react-native';

import type { Categoria } from '@/domain/entities/Categoria';
import { useProductoDraftStore } from '@/presentation/stores/productoDraftStore';
import { cn } from '@/shared/utils/cn';

interface Props {
  readonly categorias: ReadonlyArray<Categoria>;
  readonly onRequestNew: () => void;
  readonly error?: string;
}

export function CategorySelector({ categorias, onRequestNew, error }: Props) {
  const categoriaId = useProductoDraftStore((s) => s.draft.categoriaId);
  const setDraft = useProductoDraftStore((s) => s.setDraft);
  const selected = categorias.find((c) => c.id === categoriaId);

  const handleSelect = useCallback(
    (id: number) => {
      setDraft({ categoriaId: id });
    },
    [setDraft],
  );

  return (
    <View>
      <Text className="mb-1 text-sm font-medium text-surface-700 dark:text-surface-200">
        Categoría <Text className="text-danger-500">*</Text>
      </Text>
      <View className="flex-row flex-wrap gap-2">
        {categorias.map((c) => {
          const active = c.id === categoriaId;
          return (
            <Pressable
              key={c.id}
              onPress={() => handleSelect(c.id)}
              className={cn(
                'rounded-full border px-3 py-2',
                active
                  ? 'border-primary-600 bg-primary-600'
                  : 'border-surface-300 bg-white dark:border-surface-700 dark:bg-surface-900',
              )}
              accessibilityRole="button"
            >
              <Text
                className={cn(
                  'text-sm font-semibold',
                  active ? 'text-white' : 'text-surface-800 dark:text-surface-100',
                )}
              >
                {c.nombre}
              </Text>
            </Pressable>
          );
        })}
        <Pressable
          onPress={onRequestNew}
          className="flex-row items-center gap-1 rounded-full border border-dashed border-primary-500 bg-primary-50 px-3 py-2 active:bg-primary-100"
        >
          <Text className="text-base font-bold text-primary-700">+</Text>
          <Text className="text-sm font-semibold text-primary-700">Nueva</Text>
        </Pressable>
      </View>
      {error ? <Text className="mt-1 text-xs text-danger-500">{error}</Text> : null}
      {selected ? null : (
        <Text className="mt-1 text-xs text-surface-500">Selecciona una categoría obligatoria.</Text>
      )}
    </View>
  );
}
