import { useCallback, useState } from 'react';

import { useSettingsStore } from '@/presentation/stores/settingsStore';
import { SettingsStore } from '@/infrastructure/settings/SettingsStore';
import { ToastService } from '@/infrastructure/toast/ToastService';

export function useSettingsPageSize() {
  const pageSize = useSettingsStore((s) => s.pageSize);
  const setPageSize = useSettingsStore((s) => s.setPageSize);
  const [draft, setDraft] = useState(String(pageSize));

  const save = useCallback(() => {
    const n = Math.max(1, Math.min(100, Math.floor(Number(draft) || 15)));
    setPageSize(n);
    const cur = useSettingsStore.getState();
    void SettingsStore.save({
      currency: cur.currency,
      pageSize: n,
      biometricEnabled: cur.biometricEnabled,
    });
    setDraft(String(n));
    ToastService.success('Paginación actualizada', `${n} por página`);
  }, [draft, setPageSize]);

  return { draft, setDraft, save };
}
