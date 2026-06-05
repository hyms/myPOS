import React, { memo, useCallback } from 'react';
import { Pressable, ScrollView, Text } from 'react-native';

import type { Categoria } from '@/domain/entities/Categoria';
import { cn } from '@/shared/utils/cn';

interface Props {
  readonly categorias: ReadonlyArray<Categoria>;
  readonly selectedId: number | null;
  readonly onSelect: (id: number | null) => void;
}

function CategoryChipsRowComponent({ categorias, selectedId, onSelect }: Props) {
  const handleAll = useCallback(() => onSelect(null), [onSelect]);

  return (
    <ScrollView
      horizontal
      showsHorizontalScrollIndicator={false}
      contentContainerClassName="gap-2 px-3 py-2"
    >
      <Chip label="Todas" active={selectedId === null} onPress={handleAll} />
      {categorias.map((c) => (
        <Chip
          key={c.id}
          label={c.nombre}
          active={selectedId === c.id}
          onPress={() => onSelect(c.id)}
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
      className={cn(
        'rounded-full border px-3 py-1.5',
        active
          ? 'border-accent bg-accent'
          : 'border-border bg-surface',
      )}
    >
      <Text
        className={cn(
          'text-sm font-medium',
          active ? 'text-onAccent' : 'text-ink',
        )}
      >
        {label}
      </Text>
    </Pressable>
  );
}

export const CategoryChipsRow = memo(CategoryChipsRowComponent);
