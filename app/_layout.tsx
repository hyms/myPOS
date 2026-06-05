import '../global.css';

import React, { useEffect, useState } from 'react';
import { Stack, SplashScreen, useRouter } from 'expo-router';
import { GestureHandlerRootView } from 'react-native-gesture-handler';
import { SafeAreaProvider } from 'react-native-safe-area-context';
import { StatusBar } from 'expo-status-bar';
import { View } from 'react-native';
import Toast from 'react-native-toast-message';

import { getDatabase } from '@/data/database/client';
import { getRepositories } from '@/data/repositories/container';
import { useDatabaseStatus } from '@/presentation/hooks/useDatabase';
import { useSettingsStore } from '@/presentation/stores/settingsStore';
import { useSessionStore } from '@/presentation/stores/sessionStore';
import { SettingsStore } from '@/infrastructure/settings/SettingsStore';
import { PinService } from '@/infrastructure/security/PinService';
import { BiometricService } from '@/infrastructure/security/BiometricService';
import { APP_NAME } from '@/shared/constants';
import { CustomToast } from '@/presentation/components/feedback/Toast';

SplashScreen.preventAutoHideAsync().catch(() => {});

export default function RootLayout() {
  const { ready, error } = useDatabaseStatus();
  const hydrate = useSettingsStore((s) => s.hydrate);
  const hydrated = useSettingsStore((s) => s.hydrated);
  const setPinRequired = useSessionStore((s) => s.setPinRequired);
  const unlocked = useSessionStore((s) => s.unlocked);
  const router = useRouter();
  const [bootstrapped, setBootstrapped] = useState(false);

  useEffect(() => {
    if (!ready) return;
    (async () => {
      try {
        const settings = await SettingsStore.load();
        hydrate(settings);
        const hasPin = await PinService.hasPin();
        setPinRequired(hasPin);
        if (!hasPin) {
          useSessionStore.getState().setUnlocked(true);
        }
        setBootstrapped(true);
        await SplashScreen.hideAsync().catch(() => {});
      } catch {
        setBootstrapped(true);
        await SplashScreen.hideAsync().catch(() => {});
      }
    })();
  }, [ready, hydrate, setPinRequired]);

  useEffect(() => {
    if (!bootstrapped) return;
    if (useSessionStore.getState().pinRequired && !unlocked) {
      router.replace('/lock');
    } else {
      router.replace('/(tabs)');
    }
  }, [bootstrapped, router, unlocked]);

  if (!ready || !hydrated || !bootstrapped) {
    return <View style={{ flex: 1, backgroundColor: '#0f172a' }} />;
  }

  if (error) {
    return (
      <View style={{ flex: 1, alignItems: 'center', justifyContent: 'center', padding: 24 }}>
        <Stack.Screen options={{ title: 'Error' }} />
      </View>
    );
  }

  return (
    <GestureHandlerRootView style={{ flex: 1 }}>
      <SafeAreaProvider>
        <StatusBar style="light" />
        <Stack
          screenOptions={{
            headerStyle: { backgroundColor: '#0f172a' },
            headerTintColor: '#f8fafc',
            headerTitleStyle: { fontWeight: '700' },
            contentStyle: { backgroundColor: '#f8fafc' },
          }}
        >
          <Stack.Screen name="(tabs)" options={{ headerShown: false }} />
          <Stack.Screen name="lock" options={{ headerShown: false, gestureEnabled: false }} />
          <Stack.Screen
            name="productos/nuevo"
            options={{ title: 'Nuevo producto', presentation: 'modal' }}
          />
          <Stack.Screen
            name="productos/[id]"
            options={{ title: 'Editar producto', presentation: 'modal' }}
          />
          <Stack.Screen
            name="gastos/index"
            options={{ title: 'Gastos' }}
          />
          <Stack.Screen
            name="gastos/nuevo"
            options={{ title: 'Nuevo gasto', presentation: 'modal' }}
          />
          <Stack.Screen
            name="transacciones/[id]"
            options={{ title: 'Detalle' }}
          />
          <Stack.Screen
            name="settings/index"
            options={{ title: APP_NAME }}
          />
        </Stack>
        <Toast
          config={{
            custom: (p) => <CustomToast {...p} />,
          }}
        />
      </SafeAreaProvider>
    </GestureHandlerRootView>
  );
}

export { };
