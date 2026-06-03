import React from 'react';
import { Pressable, Text, View } from 'react-native';
import { router } from 'expo-router';

import { Card } from '@/presentation/components/ui/Card';
import { APP_NAME } from '@/shared/constants';

interface MenuItem {
  readonly label: string;
  readonly glyph: string;
  readonly route: string;
  readonly description: string;
}

const ITEMS: ReadonlyArray<MenuItem> = [
  { label: 'Gastos', glyph: '🧾', route: '/gastos', description: 'Registra y consulta gastos.' },
  { label: 'Configuración', glyph: '⚙️', route: '/settings', description: 'PIN, biometría, backup y más.' },
];

export default function MasTab() {
  return (
    <View className="flex-1 bg-surface-50 p-4 dark:bg-surface-950">
      <Text className="mb-1 text-2xl font-bold text-surface-900 dark:text-surface-50">
        Más opciones
      </Text>
      <Text className="mb-4 text-sm text-surface-500">{APP_NAME}</Text>
      <View className="gap-3">
        {ITEMS.map((item) => (
          <Card
            key={item.route}
            onPress={() => router.push(item.route as never)}
            className="flex-row items-center"
          >
            <Text className="mr-3 text-2xl">{item.glyph}</Text>
            <View className="flex-1">
              <Text className="text-base font-bold text-surface-900 dark:text-surface-50">{item.label}</Text>
              <Text className="text-xs text-surface-500">{item.description}</Text>
            </View>
            <Text className="text-2xl text-surface-400">›</Text>
          </Card>
        ))}
      </View>
    </View>
  );
}
