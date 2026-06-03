import React from 'react';
import { Pressable, Text, View } from 'react-native';

import { useSettingsStore } from '@/presentation/stores/settingsStore';
import { CURRENCY_OPTIONS } from '@/domain/value-objects/Currency';
import { Card } from '@/presentation/components/ui/Card';
import { SettingsStore } from '@/infrastructure/settings/SettingsStore';
import { ToastService } from '@/infrastructure/toast/ToastService';
import { cn } from '@/shared/utils/cn';

export default function SettingsDivisa() {
  const currency = useSettingsStore((s) => s.currency);
  const setCurrency = useSettingsStore((s) => s.setCurrency);

  const handleSelect = (value: typeof currency) => {
    setCurrency(value);
    const cur = useSettingsStore.getState();
    SettingsStore.save({
      currency: cur.currency,
      pageSize: cur.pageSize,
      biometricEnabled: cur.biometricEnabled,
    });
    ToastService.success('Divisa actualizada', value);
  };

  return (
    <View className="flex-1 gap-3 bg-surface-50 p-4 dark:bg-surface-950">
      <Text className="text-sm text-surface-500">Formato visual de los montos.</Text>
      {CURRENCY_OPTIONS.map((opt) => {
        const active = opt === currency;
        return (
          <Pressable
            key={opt}
            onPress={() => handleSelect(opt)}
            className={cn(
              'rounded-2xl border p-4',
              active
                ? 'border-primary-600 bg-primary-50'
                : 'border-surface-200 bg-white dark:border-surface-700 dark:bg-surface-900',
            )}
          >
            <Text
              className={cn(
                'text-base font-bold',
                active ? 'text-primary-700' : 'text-surface-900 dark:text-surface-50',
              )}
            >
              {opt}
            </Text>
          </Pressable>
        );
      })}
    </View>
  );
}
