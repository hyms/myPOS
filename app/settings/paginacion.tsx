import React, { useCallback, useState } from 'react';
import { View, Text } from 'react-native';

import { useSettingsStore } from '@/presentation/stores/settingsStore';
import { Input } from '@/presentation/components/ui/Input';
import { Button } from '@/presentation/components/ui/Button';
import { SettingsStore } from '@/infrastructure/settings/SettingsStore';
import { ToastService } from '@/infrastructure/toast/ToastService';

export default function SettingsPaginacion() {
  const pageSize = useSettingsStore((s) => s.pageSize);
  const setPageSize = useSettingsStore((s) => s.setPageSize);
  const [value, setValue] = useState(String(pageSize));

  const handleSave = useCallback(() => {
    const n = Math.max(1, Math.min(100, Math.floor(Number(value) || 15)));
    setPageSize(n);
    const cur = useSettingsStore.getState();
    SettingsStore.save({
      currency: cur.currency,
      pageSize: n,
      biometricEnabled: cur.biometricEnabled,
    });
    setValue(String(n));
    ToastService.success('Paginación actualizada', `${n} por página`);
  }, [setPageSize, value]);

  return (
    <View className="flex-1 gap-4 bg-surface-50 p-4 dark:bg-surface-950">
      <Text className="text-sm text-surface-500">
        Cantidad de filas por consulta en las listas históricas (gastos, transacciones, etc.).
      </Text>
      <Input
        label="Tamaño de página"
        value={value}
        onChangeText={(t) => setValue(t.replace(/[^0-9]/g, ''))}
        keyboardType="number-pad"
      />
      <Button title="Guardar" onPress={handleSave} fullWidth />
    </View>
  );
}
