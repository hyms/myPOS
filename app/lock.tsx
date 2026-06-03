import React, { useCallback, useEffect, useRef, useState } from 'react';
import { Pressable, Text, TextInput, View } from 'react-native';
import { useRouter } from 'expo-router';

import { Button } from '@/presentation/components/ui/Button';
import { Input } from '@/presentation/components/ui/Input';
import { PinService } from '@/infrastructure/security/PinService';
import { BiometricService } from '@/infrastructure/security/BiometricService';
import { useSettingsStore } from '@/presentation/stores/settingsStore';
import { useSessionStore } from '@/presentation/stores/sessionStore';
import { ToastService } from '@/infrastructure/toast/ToastService';

export default function LockScreen() {
  const router = useRouter();
  const [pin, setPin] = useState('');
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);
  const inputRef = useRef<TextInput>(null);
  const setUnlocked = useSessionStore((s) => s.setUnlocked);
  const biometricEnabled = useSettingsStore((s) => s.biometricEnabled);

  const tryBiometric = useCallback(async () => {
    const ok = await BiometricService.authenticate('Desbloquear TPV');
    if (ok) {
      setUnlocked(true);
      router.replace('/(tabs)');
    } else {
      ToastService.info('Cancelado', 'Ingresa tu PIN.');
    }
  }, [router, setUnlocked]);

  useEffect(() => {
    if (biometricEnabled) {
      tryBiometric().catch(() => {});
    }
  }, [biometricEnabled, tryBiometric]);

  const handleUnlock = useCallback(async () => {
    if (pin.length < 4) {
      setError('PIN muy corto.');
      return;
    }
    setBusy(true);
    try {
      const ok = await PinService.verifyPin(pin);
      if (!ok) {
        setError('PIN incorrecto.');
        return;
      }
      setUnlocked(true);
      router.replace('/(tabs)');
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Error de autenticación.');
    } finally {
      setBusy(false);
    }
  }, [pin, router, setUnlocked]);

  return (
    <View className="flex-1 items-center justify-center bg-surface-900 p-6">
      <Text className="mb-2 text-3xl font-bold text-white">🔒</Text>
      <Text className="mb-6 text-xl font-bold text-white">TPV Móvil Express</Text>
      <View className="w-full max-w-sm">
        <Input
          ref={inputRef}
          label="PIN"
          value={pin}
          onChangeText={(t) => {
            setError(null);
            setPin(t.replace(/[^0-9]/g, '').slice(0, 6));
          }}
          keyboardType="number-pad"
          secureTextEntry
          autoFocus
          error={error ?? undefined}
          onSubmitEditing={handleUnlock}
          placeholder="••••"
        />
        <View className="mt-4 gap-2">
          <Button title="Desbloquear" onPress={handleUnlock} busy={busy} disabled={busy} fullWidth />
          {biometricEnabled ? (
            <Pressable
              onPress={tryBiometric}
              className="rounded-lg bg-surface-800 px-4 py-3 active:bg-surface-700"
            >
              <Text className="text-center font-semibold text-white">Usar biometría</Text>
            </Pressable>
          ) : null}
        </View>
      </View>
    </View>
  );
}
