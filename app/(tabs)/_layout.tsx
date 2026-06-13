import React from 'react';
import { Tabs } from 'expo-router';
import { Text, View } from 'react-native';

import {
  useCompraCarritoStore,
  useVentaCarritoStore,
} from '@/presentation/stores/carritoStore';
import { useRegistroSubtabStore } from '@/presentation/stores/registroSubtabStore';
import { Icon } from '@/presentation/components/ui/Icon';
import type { IconName } from '@/presentation/components/ui/Icon';
import { DARK_PALETTE } from '@/presentation/theme/tokens';

const TAB_TINT_ACTIVE = DARK_PALETTE.ink;
const TAB_TINT_INACTIVE = DARK_PALETTE.inkMuted;
const TAB_BG = DARK_PALETTE.surface;
const TAB_BORDER = DARK_PALETTE.border;

function TabIcon({
  name,
  focused,
  badge,
}: {
  name: IconName;
  focused: boolean;
  badge?: number;
}) {
  return (
    <View style={{ position: 'relative' }}>
      <Icon
        name={name}
        size={22}
        color={focused ? TAB_TINT_ACTIVE : TAB_TINT_INACTIVE}
      />
      {badge && badge > 0 ? (
        <View
          style={{
            position: 'absolute',
            right: -8,
            top: -4,
            backgroundColor: DARK_PALETTE.danger,
            borderRadius: 999,
            minWidth: 16,
            height: 16,
            paddingHorizontal: 4,
            alignItems: 'center',
            justifyContent: 'center',
          }}
        >
          <Text style={{ color: DARK_PALETTE.inkStrong, fontSize: 10, fontWeight: '700' }}>{badge}</Text>
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
        tabBarActiveTintColor: TAB_TINT_ACTIVE,
        tabBarInactiveTintColor: TAB_TINT_INACTIVE,
        tabBarStyle: {
          backgroundColor: TAB_BG,
          borderTopColor: TAB_BORDER,
          borderTopWidth: 0.5,
        },
        headerStyle: { backgroundColor: DARK_PALETTE.surface },
        headerTintColor: DARK_PALETTE.ink,
        headerTitleStyle: { fontWeight: '700' },
        headerShadowVisible: false,
      }}
    >
      <Tabs.Screen
        name="caja"
        options={{
          title: 'Cuentas',
          tabBarIcon: ({ focused }) => <TabIcon name="cash-outline" focused={focused} />,
        }}
      />
      <Tabs.Screen
        name="registro"
        options={{
          title: 'Registro',
          tabBarIcon: ({ focused }) => <TabIcon name="receipt-outline" focused={focused} badge={registroBadge} />,
        }}
      />
      <Tabs.Screen
        name="productos"
        options={{
          title: 'Productos',
          tabBarIcon: ({ focused }) => <TabIcon name="cube-outline" focused={focused} />,
        }}
      />
      <Tabs.Screen
        name="mas"
        options={{
          title: 'Más',
          tabBarIcon: ({ focused }) => <TabIcon name="ellipsis-horizontal" focused={focused} />,
        }}
      />
    </Tabs>
  );
}
