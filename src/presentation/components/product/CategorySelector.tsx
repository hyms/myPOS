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
      <Text className="mb-1 text-sm font-medium text-ink">
        Categoría <Text className="text-danger">*</Text>
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
                  ? 'border-accent bg-accent'
                  : 'border-border bg-surface',
              )}
              accessibilityRole="button"
            >
              <Text
                className={cn(
                  'text-sm font-semibold',
                  active ? 'text-onAccent' : 'text-ink-strong',
                )}
              >
                {c.nombre}
              </Text>
            </Pressable>
          );
        })}
        <Pressable
          onPress={onRequestNew}
          className="flex-row items-center gap-1 rounded-full border border-dashed border-accent bg-surface-hi px-3 py-2"
        >
          <Text className="text-base font-bold text-ink-strong">+</Text>
          <Text className="text-sm font-semibold text-ink-strong">Nueva</Text>
        </Pressable>
      </View>
      {error ? <Text className="mt-1 text-xs text-danger">{error}</Text> : null}
      {selected ? null : (
        <Text className="mt-1 text-xs text-ink-muted">Selecciona una categoría obligatoria.</Text>
      )}
    </View>
  );
}
