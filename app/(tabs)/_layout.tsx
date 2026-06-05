import React from 'react';
import { Tabs } from 'expo-router';
import { Text, View } from 'react-native';

import {
  useCompraCarritoStore,
  useVentaCarritoStore,
} from '@/presentation/stores/carritoStore';
import { useRegistroSubtabStore } from '@/presentation/stores/registroSubtabStore';

function TabIcon({ glyph, focused, badge }: { glyph: string; focused: boolean; badge?: number }) {
  return (
    <View style={{ position: 'relative' }}>
      <Text
        style={{
          fontSize: 22,
          color: focused ? '#0ea5e9' : '#64748b',
        }}
      >
        {glyph}
      </Text>
      {badge && badge > 0 ? (
        <View
          style={{
            position: 'absolute',
            right: -8,
            top: -4,
            backgroundColor: '#dc2626',
            borderRadius: 999,
            minWidth: 16,
            height: 16,
            paddingHorizontal: 4,
            alignItems: 'center',
            justifyContent: 'center',
          }}
        >
          <Text style={{ color: 'white', fontSize: 10, fontWeight: '700' }}>{badge}</Text>
        </View>
      ) : null}
    </View>
  );
}

export default function TabsLayout() {
  const ventasCount = useVentaCarritoStore((s) => s.items.length);
  const comprasCount = useCompraCarritoStore((s) => s.items.length);
  const subtab = useRegistroSubtabStore((s) => s.tipo);
  const registroBadge = subtab === 'VENTA' ? ventasCount : comprasCount;

  return (
    <Tabs
      screenOptions={{
        tabBarActiveTintColor: '#0ea5e9',
        tabBarInactiveTintColor: '#64748b',
        tabBarStyle: { borderTopColor: '#e2e8f0' },
        headerStyle: { backgroundColor: '#0f172a' },
        headerTintColor: '#f8fafc',
        headerTitleStyle: { fontWeight: '700' },
      }}
    >
      <Tabs.Screen
        name="registro"
        options={{
          title: 'Registro',
          tabBarIcon: ({ focused }) => <TabIcon glyph="📝" focused={focused} badge={registroBadge} />,
        }}
      />
      <Tabs.Screen
        name="productos"
        options={{
          title: 'Productos',
          tabBarIcon: ({ focused }) => <TabIcon glyph="📦" focused={focused} />,
        }}
      />
      <Tabs.Screen
        name="caja"
        options={{
          title: 'Caja',
          tabBarIcon: ({ focused }) => <TabIcon glyph="💵" focused={focused} />,
        }}
      />
      <Tabs.Screen
        name="mas"
        options={{
          title: 'Más',
          tabBarIcon: ({ focused }) => <TabIcon glyph="⋯" focused={focused} />,
        }}
      />
    </Tabs>
  );
}
