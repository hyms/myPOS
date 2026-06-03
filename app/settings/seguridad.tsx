import React, { useCallback, useEffect, useState } from 'react';
import { Pressable, Switch, Text, View } from 'react-native';

import { Input } from '@/presentation/components/ui/Input';
import { Button } from '@/presentation/components/ui/Button';
import { PinService } from '@/infrastructure/security/PinService';
import { BiometricService } from '@/infrastructure/security/BiometricService';
import { useSettingsStore } from '@/presentation/stores/settingsStore';
import { SettingsStore } from '@/infrastructure/settings/SettingsStore';
import { ToastService } from '@/infrastructure/toast/ToastService';
import { ConfirmDialog } from '@/presentation/components/feedback/ConfirmDialog';
import { useSessionStore } from '@/presentation/stores/sessionStore';
import { Card } from '@/presentation/components/ui/Card';
import { cn } from '@/shared/utils/cn';

export default function SettingsSeguridad() {
  const [hasPin, setHasPin] = useState(false);
  const [currentPin, setCurrentPin] = useState('');
  const [newPin, setNewPin] = useState('');
  const [confirmPin, setConfirmPin] = useState('');
  const [error, setError] = useState<string | null>(null);
  const [biometricAvailable, setBiometricAvailable] = useState(false);
  const [clearOpen, setClearOpen] = useState(false);
  const biometricEnabled = useSettingsStore((s) => s.biometricEnabled);
  const setBiometricEnabled = useSettingsStore((s) => s.setBiometricEnabled);
  const setPinRequired = useSessionStore((s) => s.setPinRequired);
  const setUnlocked = useSessionStore((s) => s.setUnlocked);

  useEffect(() => {
    (async () => {
      setHasPin(await PinService.hasPin());
      setBiometricAvailable(await BiometricService.isAvailable());
    })();
  }, []);

  const persistBiometric = useCallback(
    async (value: boolean) => {
      setBiometricEnabled(value);
      const cur = useSettingsStore.getState();
      await SettingsStore.save({
        currency: cur.currency,
        pageSize: cur.pageSize,
        biometricEnabled: value,
      });
    },
    [setBiometricEnabled],
  );

  const handleBiometric = useCallback(
    async (next: boolean) => {
      if (!next) {
        await persistBiometric(false);
        ToastService.info('Biometría desactivada');
        return;
      }
      if (!biometricAvailable) {
        ToastService.error('No disponible', 'Tu dispositivo no soporta biometría.');
        return;
      }
      const ok = await BiometricService.authenticate('Activar biometría');
      if (!ok) {
        ToastService.error('Cancelado');
        return;
      }
      await persistBiometric(true);
      ToastService.success('Biometría activada');
    },
    [biometricAvailable, persistBiometric],
  );

  const handleSetPin = useCallback(async () => {
    setError(null);
    if (newPin.length < 4) {
      setError('El PIN debe tener al menos 4 dígitos.');
      return;
    }
    if (newPin !== confirmPin) {
      setError('La confirmación no coincide.');
      return;
    }
    if (hasPin) {
      const ok = await PinService.verifyPin(currentPin);
      if (!ok) {
        setError('PIN actual incorrecto.');
        return;
      }
    }
    try {
      await PinService.setPin(newPin);
      setHasPin(true);
      setPinRequired(true);
      setUnlocked(true);
      setCurrentPin('');
      setNewPin('');
      setConfirmPin('');
      ToastService.success(hasPin ? 'PIN actualizado' : 'PIN configurado');
    } catch (e) {
      setError(e instanceof Error ? e.message : 'No se pudo guardar el PIN.');
    }
  }, [confirmPin, currentPin, hasPin, newPin, setPinRequired, setUnlocked]);

  const handleClear = useCallback(async () => {
    await PinService.clearPin();
    setHasPin(false);
    setPinRequired(false);
    setUnlocked(true);
    setClearOpen(false);
    ToastService.info('PIN eliminado');
  }, [setPinRequired, setUnlocked]);

  return (
    <View className="flex-1 gap-4 bg-surface-50 p-4 dark:bg-surface-950">
      <Card className="flex-row items-center justify-between">
        <View className="flex-1 pr-3">
          <Text className="text-base font-semibold text-surface-900 dark:text-surface-50">Biometría</Text>
          <Text className="text-xs text-surface-500">
            {biometricAvailable ? 'Disponible en este dispositivo.' : 'No disponible en este dispositivo.'}
          </Text>
        </View>
        <Switch
          value={biometricEnabled}
          onValueChange={handleBiometric}
          disabled={!biometricAvailable}
        />
      </Card>

      <View className="gap-3">
        <Text className="text-sm font-semibold text-surface-700 dark:text-surface-200">
          {hasPin ? 'Cambiar PIN' : 'Configurar PIN'}
        </Text>
        {hasPin ? (
          <Input
            label="PIN actual"
            value={currentPin}
            onChangeText={(t) => setCurrentPin(t.replace(/[^0-9]/g, '').slice(0, 6))}
            keyboardType="number-pad"
            secureTextEntry
          />
        ) : null}
        <Input
          label="Nuevo PIN (4-6 dígitos)"
          value={newPin}
          onChangeText={(t) => setNewPin(t.replace(/[^0-9]/g, '').slice(0, 6))}
          keyboardType="number-pad"
          secureTextEntry
        />
        <Input
          label="Confirmar nuevo PIN"
          value={confirmPin}
          onChangeText={(t) => setConfirmPin(t.replace(/[^0-9]/g, '').slice(0, 6))}
          keyboardType="number-pad"
          secureTextEntry
          error={error ?? undefined}
        />
        <Button title={hasPin ? 'Actualizar PIN' : 'Guardar PIN'} onPress={handleSetPin} fullWidth />
      </View>

      {hasPin ? (
        <Button
          title="Eliminar PIN"
          variant="danger"
          onPress={() => setClearOpen(true)}
          fullWidth
        />
      ) : null}

      <ConfirmDialog
        visible={clearOpen}
        title="Eliminar PIN"
        message="¿Estás seguro? La app quedará desbloqueada hasta que configures uno nuevo."
        destructive
        onCancel={() => setClearOpen(false)}
        onConfirm={handleClear}
      />
    </View>
  );
}
