import { useCallback, useEffect, useState } from 'react';

import { useSettingsStore } from '@/presentation/stores/settingsStore';
import { SettingsStore } from '@/infrastructure/settings/SettingsStore';
import { ToastService } from '@/infrastructure/toast/ToastService';
import { PinService } from '@/infrastructure/security/PinService';
import { BiometricService } from '@/infrastructure/security/BiometricService';
import { useSessionStore } from '@/presentation/stores/sessionStore';

export function useSettingsSecurity() {
  const biometricEnabled = useSettingsStore((s) => s.biometricEnabled);
  const setBiometricEnabled = useSettingsStore((s) => s.setBiometricEnabled);
  const setPinRequired = useSessionStore((s) => s.setPinRequired);
  const setUnlocked = useSessionStore((s) => s.setUnlocked);

  const [hasPin, setHasPin] = useState(false);
  const [biometricAvailable, setBiometricAvailable] = useState(false);
  const [currentPin, setCurrentPin] = useState('');
  const [newPin, setNewPin] = useState('');
  const [confirmPin, setConfirmPin] = useState('');
  const [error, setError] = useState<string | null>(null);
  const [clearOpen, setClearOpen] = useState(false);

  useEffect(() => {
    void (async () => {
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

  const toggleBiometric = useCallback(
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

  const savePin = useCallback(async () => {
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

  const clearPin = useCallback(async () => {
    await PinService.clearPin();
    setHasPin(false);
    setPinRequired(false);
    setUnlocked(true);
    setClearOpen(false);
    ToastService.info('PIN eliminado');
  }, [setPinRequired, setUnlocked]);

  return {
    hasPin,
    biometricAvailable,
    biometricEnabled,
    toggleBiometric,
    currentPin,
    setCurrentPin,
    newPin,
    setNewPin,
    confirmPin,
    setConfirmPin,
    error,
    savePin,
    clearOpen,
    setClearOpen,
    clearPin,
  };
}
