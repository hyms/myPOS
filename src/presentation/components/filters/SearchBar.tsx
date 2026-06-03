import React, { memo } from 'react';
import { Pressable, Text, TextInput, View } from 'react-native';

import { cn } from '@/shared/utils/cn';

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
        'flex-row items-center rounded-xl border border-surface-200 bg-white px-3 dark:border-surface-700 dark:bg-surface-900',
        className,
      )}
    >
      <Text className="mr-2 text-base text-surface-400">🔍</Text>
      <TextInput
        value={value}
        onChangeText={onChangeText}
        placeholder={placeholder ?? 'Buscar…'}
        placeholderTextColor="#94a3b8"
        className="flex-1 py-2.5 text-base text-surface-900 dark:text-surface-50"
        returnKeyType="search"
        autoCorrect={false}
      />
      {value.length > 0 ? (
        <Pressable onPress={() => onChangeText('')} hitSlop={8}>
          <Text className="px-2 text-lg text-surface-400">×</Text>
        </Pressable>
      ) : null}
    </View>
  );
}

export const SearchBar = memo(SearchBarComponent);
