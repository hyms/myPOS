import React from 'react';
import { Text, View } from 'react-native';
import { router } from 'expo-router';
import { Card } from '@/presentation/components/ui/Card';
import { AnimatedPressable } from '@/presentation/components/ui/AnimatedPressable';
import { Icon } from '@/presentation/components/ui/Icon';
import type { IconName } from '@/presentation/components/ui/Icon';
import { APP_NAME } from '@/shared/constants';

interface MenuItem { readonly label: string; readonly icon: IconName; readonly route: string; readonly description: string; }

const ITEMS: ReadonlyArray<MenuItem> = [
  { label: 'Movimientos', icon: 'swap-vertical-outline', route: '/movimientos', description: 'Ventas, compras y gastos en una lista.' },
  { label: 'Gastos', icon: 'receipt-outline', route: '/gastos', description: 'Registra y consulta gastos.' },
  { label: 'Configuración', icon: 'settings-outline', route: '/settings', description: 'PIN, biometría, backup y más.' },
];

export default function MasTab() {
  return (
    <View className="flex-1 bg-surface-50 p-4 dark:bg-surface-950">
      <Text className="mb-1 text-2xl font-bold text-surface-900 dark:text-surface-50">Más opciones</Text>
      <Text className="mb-4 text-sm text-surface-500">{APP_NAME}</Text>
      <View className="gap-3">
        {ITEMS.map((item) => (
          <AnimatedPressable key={item.route} onPress={() => router.push(item.route as never)}>
            <Card className="flex-row items-center">
              <View className="mr-3 rounded-xl bg-primary-50 p-2.5 dark:bg-primary-950">
                <Icon name={item.icon} size={22} color="#f59e0b" />
              </View>
              <View className="flex-1">
                <Text className="text-base font-bold text-surface-900 dark:text-surface-50">{item.label}</Text>
                <Text className="text-xs text-surface-500">{item.description}</Text>
              </View>
              <Icon name="chevron-forward" size={20} color="#94a3b8" />
            </Card>
          </AnimatedPressable>
        ))}
      </View>
    </View>
  );
}
