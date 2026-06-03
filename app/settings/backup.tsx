import React, { useState } from 'react';
import { Text, View } from 'react-native';
import { router } from 'expo-router';

import { Button } from '@/presentation/components/ui/Button';
import { ConfirmDialog } from '@/presentation/components/feedback/ConfirmDialog';
import { exportarBackup } from '@/application/backup/ExportarDB';
import { restaurarBackup } from '@/application/backup/RestaurarDB';
import { ToastService } from '@/infrastructure/toast/ToastService';
import { useResumenCaja } from '@/presentation/hooks/useResumenCaja';
import { useSessionStore } from '@/presentation/stores/sessionStore';
import { useSettingsStore } from '@/presentation/stores/settingsStore';

export default function SettingsBackup() {
  const [busy, setBusy] = useState<'export' | 'restore' | null>(null);
  const [restoreOpen, setRestoreOpen] = useState(false);
  const { refresh: refreshCaja } = useResumenCaja();

  const handleExport = async () => {
    try {
      setBusy('export');
      await exportarBackup();
      ToastService.success('Copia lista para compartir');
    } catch (e) {
      ToastService.error('Error', e instanceof Error ? e.message : 'No se pudo exportar.');
    } finally {
      setBusy(null);
    }
  };

  const handleRestore = async () => {
    try {
      setBusy('restore');
      await restaurarBackup();
      setRestoreOpen(false);
      ToastService.success('Base restaurada. Reiniciando…');
      // Force re-hydrate settings + session to start fresh.
      setTimeout(() => {
        useSessionStore.getState().setUnlocked(true);
        useSettingsStore.setState({ hydrated: false });
        refreshCaja();
        router.replace('/(tabs)');
      }, 500);
    } catch (e) {
      ToastService.error('Error', e instanceof Error ? e.message : 'No se pudo restaurar.');
      setBusy(null);
    }
  };

  return (
    <View className="flex-1 gap-4 bg-surface-50 p-4 dark:bg-surface-950">
      <Text className="text-sm text-surface-500">
        Exporta una copia de la base de datos a tu dispositivo. Restaurar sobrescribirá todos los
        datos actuales.
      </Text>
      <Button title="Exportar base de datos" onPress={handleExport} busy={busy === 'export'} fullWidth />
      <Button
        title="Restaurar desde archivo…"
        variant="danger"
        onPress={() => setRestoreOpen(true)}
        busy={busy === 'restore'}
        fullWidth
      />
      <ConfirmDialog
        visible={restoreOpen}
        title="Restaurar base de datos"
        message="Esto sobrescribirá todos los datos actuales. ¿Continuar?"
        destructive
        onCancel={() => setRestoreOpen(false)}
        onConfirm={handleRestore}
        busy={busy === 'restore'}
      />
    </View>
  );
}
