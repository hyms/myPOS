import React from 'react';
import { Tabs } from 'expo-router';
import { Text, View } from 'react-native';

import { useCarritoCount } from '@/presentation/hooks/useCarrito';

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
  const ventasCount = useCarritoCount();
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
        name="index"
        options={{
          title: 'Ventas',
          tabBarIcon: ({ focused }) => <TabIcon glyph="💰" focused={focused} badge={ventasCount} />,
        }}
      />
      <Tabs.Screen
        name="compras"
        options={{
          title: 'Compras',
          tabBarIcon: ({ focused }) => <TabIcon glyph="🛒" focused={focused} />,
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
