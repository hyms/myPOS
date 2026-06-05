import React from 'react';
import { Tabs } from 'expo-router';
import { View, Text } from 'react-native';
import { useCompraCarritoStore, useVentaCarritoStore } from '@/presentation/stores/carritoStore';
import { Icon } from '@/presentation/components/ui/Icon';
import type { IconName } from '@/presentation/components/ui/Icon';

interface TabConfig { readonly icon: IconName; readonly activeIcon: IconName; }

const TAB_ICONS: Record<string, TabConfig> = {
  index: { icon: 'cash-outline', activeIcon: 'cash' },
  compras: { icon: 'cart-outline', activeIcon: 'cart' },
  productos: { icon: 'cube-outline', activeIcon: 'cube' },
  caja: { icon: 'wallet-outline', activeIcon: 'wallet' },
  mas: { icon: 'ellipsis-horizontal-circle-outline', activeIcon: 'ellipsis-horizontal-circle' },
};

function TabIcon({ routeName, focused, badge }: { routeName: string; focused: boolean; badge?: number }) {
  const config = TAB_ICONS[routeName];
  if (!config) return null;
  return (
    <View className="relative">
      <Icon name={focused ? config.activeIcon : config.icon} size={24} color={focused ? '#f59e0b' : '#64748b'} />
      {badge && badge > 0 ? (
        <View className="absolute -right-2 -top-1 min-w-[16px] items-center justify-center rounded-full bg-danger-600 px-[3px]"
          style={{ height: 16, paddingHorizontal: 4 }}
        >
          <Text className="text-[10px] font-bold text-white">{badge}</Text>
        </View>
      ) : null}
    </View>
  );
}

export default function TabsLayout() {
  const ventasCount = useVentaCarritoStore((s) => s.items.length);
  const comprasCount = useCompraCarritoStore((s) => s.items.length);
  return (
    <Tabs screenOptions={{
      tabBarActiveTintColor: '#f59e0b',
      tabBarInactiveTintColor: '#64748b',
      tabBarStyle: { borderTopColor: '#e2e8f0' },
      tabBarLabelStyle: { fontSize: 11, fontWeight: '600' },
      headerStyle: { backgroundColor: '#0f172a' },
      headerTintColor: '#f8fafc',
      headerTitleStyle: { fontWeight: '700' },
    }}>
      <Tabs.Screen name="index" options={{
        title: 'Ventas',
        tabBarIcon: ({ focused }) => <TabIcon routeName="index" focused={focused} badge={ventasCount} />,
      }} />
      <Tabs.Screen name="compras" options={{
        title: 'Compras',
        tabBarIcon: ({ focused }) => <TabIcon routeName="compras" focused={focused} badge={comprasCount} />,
      }} />
      <Tabs.Screen name="productos" options={{
        title: 'Productos',
        tabBarIcon: ({ focused }) => <TabIcon routeName="productos" focused={focused} />,
      }} />
      <Tabs.Screen name="caja" options={{
        title: 'Caja',
        tabBarIcon: ({ focused }) => <TabIcon routeName="caja" focused={focused} />,
      }} />
      <Tabs.Screen name="mas" options={{
        title: 'Más',
        tabBarIcon: ({ focused }) => <TabIcon routeName="mas" focused={focused} />,
      }} />
    </Tabs>
  );
}
