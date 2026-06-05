import React, { memo, useCallback } from 'react';
import { Pressable, Text, View } from 'react-native';

import { CURRENCY_OPTIONS, type CurrencyCode } from '@/domain/value-objects/Currency';
import { cn } from '@/shared/utils/cn';
import { useSettingsCurrency } from '@/presentation/hooks/useSettingsCurrency';

interface CurrencyOptionProps {
  readonly value: CurrencyCode;
  readonly active: boolean;
  readonly onSelect: (value: CurrencyCode) => void;
}

const CurrencyOption = memo(function CurrencyOption({ value, active, onSelect }: CurrencyOptionProps) {
  const handlePress = useCallback(() => onSelect(value), [onSelect, value]);
  return (
    <Pressable
      onPress={handlePress}
      className={cn(
        'flex-1 items-center rounded-xl border p-3',
        active
          ? 'border-accent bg-surface-hi'
          : 'border-border-subtle bg-surface border-border bg-surface',
      )}
    >
      <Text
        className={cn(
          'text-base font-bold',
          active ? 'text-ink-strong' : 'text-ink-strong',
        )}
      >
        {value}
      </Text>
    </Pressable>
  );
});

function DivisaSectionComponent() {
  const { currency, select } = useSettingsCurrency();
  return (
    <View className="flex-row gap-2">
      {CURRENCY_OPTIONS.map((opt) => (
        <CurrencyOption key={opt} value={opt} active={opt === currency} onSelect={select} />
      ))}
    </View>
  );
}

export const DivisaSection = memo(DivisaSectionComponent);
