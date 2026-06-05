import React, { memo, useCallback, useMemo } from 'react';
import { Switch, Text, View } from 'react-native';

import { Input } from '@/presentation/components/ui/Input';
import { Button } from '@/presentation/components/ui/Button';
import { Card } from '@/presentation/components/ui/Card';
import { ConfirmDialog } from '@/presentation/components/feedback/ConfirmDialog';
import { useSettingsSecurity } from '@/presentation/hooks/useSettingsSecurity';

const sanitizeDigits = (max: number) => (t: string) => t.replace(/[^0-9]/g, '').slice(0, max);

function BiometricRowComponent() {
  const { biometricEnabled, biometricAvailable, toggleBiometric } = useSettingsSecurity();
  return (
    <Card className="flex-row items-center justify-between">
      <View className="flex-1 pr-3">
        <Text className="text-base font-semibold text-surface-900 dark:text-surface-50">Biometría</Text>
        <Text className="text-xs text-surface-500">
          {biometricAvailable ? 'Disponible en este dispositivo.' : 'No disponible en este dispositivo.'}
        </Text>
      </View>
      <Switch
        value={biometricEnabled}
        onValueChange={toggleBiometric}
        disabled={!biometricAvailable}
      />
    </Card>
  );
}

const BiometricRow = memo(BiometricRowComponent);

function PinFormComponent() {
  const {
    hasPin,
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
  } = useSettingsSecurity();

  const onCancel = useCallback(() => setClearOpen(false), [setClearOpen]);
  const onCurrentPin = useMemo(() => sanitizeDigits(6), []);
  const onNewPin = useMemo(() => sanitizeDigits(6), []);
  const onConfirmPin = useMemo(() => sanitizeDigits(6), []);

  return (
    <>
      <Card className="gap-3">
        <Text className="text-sm font-semibold text-surface-700 dark:text-surface-200">
          {hasPin ? 'Cambiar PIN' : 'Configurar PIN'}
        </Text>
        {hasPin ? (
          <Input
            label="PIN actual"
            value={currentPin}
            onChangeText={onCurrentPin}
            keyboardType="number-pad"
            secureTextEntry
          />
        ) : null}
        <Input
          label="Nuevo PIN (4-6 dígitos)"
          value={newPin}
          onChangeText={onNewPin}
          keyboardType="number-pad"
          secureTextEntry
        />
        <Input
          label="Confirmar nuevo PIN"
          value={confirmPin}
          onChangeText={onConfirmPin}
          keyboardType="number-pad"
          secureTextEntry
          {...(error ? { error } : {})}
        />
        <Button title={hasPin ? 'Actualizar PIN' : 'Guardar PIN'} onPress={savePin} fullWidth />
        {hasPin ? (
          <Button
            title="Eliminar PIN"
            variant="danger"
            onPress={() => setClearOpen(true)}
            fullWidth
          />
        ) : null}
      </Card>
      <ConfirmDialog
        visible={clearOpen}
        title="Eliminar PIN"
        message="¿Estás seguro? La app quedará desbloqueada hasta que configures uno nuevo."
        destructive
        onCancel={onCancel}
        onConfirm={clearPin}
      />
    </>
  );
}

const PinForm = memo(PinFormComponent);

function SeguridadSectionComponent() {
  return (
    <View className="gap-3">
      <BiometricRow />
      <PinForm />
    </View>
  );
}

export const SeguridadSection = memo(SeguridadSectionComponent);
