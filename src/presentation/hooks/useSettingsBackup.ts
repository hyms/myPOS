import { useCallback, useState } from 'react';
import { router } from 'expo-router';

import { ToastService } from '@/infrastructure/toast/ToastService';
import { exportarBackup } from '@/application/backup/ExportarDB';
import { restaurarBackup } from '@/application/backup/RestaurarDB';
import { useResumenCaja } from '@/presentation/hooks/useResumenCaja';
import { useSessionStore } from '@/presentation/stores/sessionStore';
import { useSettingsStore } from '@/presentation/stores/settingsStore';

export function useSettingsBackup() {
  const { refresh: refreshCaja } = useResumenCaja();
  const [busy, setBusy] = useState<'export' | 'restore' | null>(null);
  const [restoreOpen, setRestoreOpen] = useState(false);

  const exportDb = useCallback(async () => {
    try {
      setBusy('export');
      await exportarBackup();
      ToastService.success('Copia lista para compartir');
    } catch (e) {
      ToastService.error('Error', e instanceof Error ? e.message : 'No se pudo exportar.');
    } finally {
      setBusy(null);
    }
  }, []);

  const restoreDb = useCallback(async () => {
    try {
      setBusy('restore');
      await restaurarBackup();
      setRestoreOpen(false);
      ToastService.success('Base restaurada. Reiniciando…');
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
  }, [refreshCaja]);

  return { busy, restoreOpen, setRestoreOpen, exportDb, restoreDb };
}
