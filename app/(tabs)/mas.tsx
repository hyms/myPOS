import React from 'react';
import { Text, View } from 'react-native';
import { Link } from 'expo-router';
import { Card } from '@/presentation/components/ui/Card';
import { AnimatedPressable } from '@/presentation/components/ui/AnimatedPressable';
import { Icon } from '@/presentation/components/ui/Icon';
import type { IconName } from '@/presentation/components/ui/Icon';
import { APP_NAME } from '@/shared/constants';
import { DARK_PALETTE } from '@/presentation/theme/tokens';

interface MenuItem { readonly label: string; readonly icon: IconName; readonly route: '/movimientos' | '/gastos' | '/settings'; readonly description: string; }

const ITEMS: ReadonlyArray<MenuItem> = [
  { label: 'Movimientos', icon: 'swap-vertical-outline', route: '/movimientos', description: 'Ventas, compras y gastos en una lista.' },
  { label: 'Gastos', icon: 'receipt-outline', route: '/gastos', description: 'Registra y consulta gastos.' },
  { label: 'Configuración', icon: 'settings-outline', route: '/settings', description: 'PIN, biometría, backup y más.' },
];

export default function MasTab() {
  return (
    <View className="flex-1 bg-canvas p-4">
      <Text className="mb-1 text-2xl font-bold text-ink-strong">Más opciones</Text>
      <Text className="mb-4 text-sm text-ink-muted">{APP_NAME}</Text>
      <View className="gap-3">
        {ITEMS.map((item) => (
          <Link key={item.route} href={item.route} asChild>
            <AnimatedPressable
              accessibilityRole="button"
              accessibilityLabel={`Ir a ${item.label}`}
              accessibilityHint={item.description}
            >
              <Card className="flex-row items-center">
                <View className="mr-3 rounded-xl bg-surface-lo p-2.5">
                  <Icon name={item.icon} size={22} color={DARK_PALETTE.warning} />
                </View>
                <View className="flex-1">
                  <Text className="text-base font-bold text-ink-strong">{item.label}</Text>
                  <Text className="text-xs text-ink-muted">{item.description}</Text>
                </View>
                <Icon name="chevron-forward" size={20} color={DARK_PALETTE.inkMuted} />
              </Card>
            </AnimatedPressable>
          </Link>
        ))}
      </View>
    </View>
  );
}
