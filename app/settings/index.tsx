import React from 'react';
import { Text, View } from 'react-native';
import { router } from 'expo-router';

import { Card } from '@/presentation/components/ui/Card';

const ITEMS = [
  { label: 'Divisa', glyph: '💱', route: '/settings/divisa' },
  { label: 'Paginación', glyph: '📃', route: '/settings/paginacion' },
  { label: 'Seguridad', glyph: '🔐', route: '/settings/seguridad' },
  { label: 'Copia de seguridad', glyph: '💾', route: '/settings/backup' },
] as const;

export default function SettingsIndex() {
  return (
    <View className="flex-1 gap-3 bg-surface-50 p-4 dark:bg-surface-950">
      <Text className="text-sm text-surface-500">Configuración general de la app.</Text>
      {ITEMS.map((it) => (
        <Card
          key={it.route}
          onPress={() => router.push(it.route as never)}
          className="flex-row items-center"
        >
          <Text className="mr-3 text-2xl">{it.glyph}</Text>
          <Text className="flex-1 text-base font-bold text-surface-900 dark:text-surface-50">{it.label}</Text>
          <Text className="text-2xl text-surface-400">›</Text>
        </Card>
      ))}
    </View>
  );
}
