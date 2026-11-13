import React, { memo } from 'react';
import { Pressable, TextInput, View } from 'react-native';
import { Ionicons } from '@expo/vector-icons';

import { cn } from '@/shared/utils/cn';
import { DARK_PALETTE } from '@/presentation/theme/tokens';

interface SearchBarProps {
  readonly value: string;
  readonly onChangeText: (v: string) => void;
  readonly placeholder?: string;
  readonly className?: string;
}

function SearchBarComponent({ value, onChangeText, placeholder, className }: SearchBarProps) {
  return (
    <View
      className={cn(
        'flex-row items-center rounded-xl border border-border-subtle bg-surface px-3',
        className,
      )}
    >
      <View className="mr-2">
        <Ionicons name="search" size={18} color={DARK_PALETTE.inkMuted} />
      </View>
      <TextInput
        value={value}
        onChangeText={onChangeText}
        placeholder={placeholder ?? 'Buscar…'}
        placeholderTextColor={DARK_PALETTE.inkMuted}
        className="flex-1 py-2.5 text-base text-ink-strong"
        returnKeyType="search"
        autoCorrect={false}
      />
      {value.length > 0 ? (
        <Pressable
          onPress={() => onChangeText('')}
          accessibilityRole="button"
          accessibilityLabel="Limpiar búsqueda"
          hitSlop={12}
          className="min-h-[44px] min-w-[44px] items-center justify-center"
        >
          <Ionicons name="close-circle" size={18} color={DARK_PALETTE.inkMuted} />
        </Pressable>
      ) : null}
    </View>
  );
}

export const SearchBar = memo(SearchBarComponent);
