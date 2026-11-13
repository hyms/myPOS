import { useCallback } from 'react';

import { useSettingsStore } from '@/presentation/stores/settingsStore';
import { SettingsStore } from '@/infrastructure/settings/SettingsStore';
import { ToastService } from '@/infrastructure/toast/ToastService';
import type { CurrencyCode } from '@/domain/value-objects/Currency';

export function useSettingsCurrency() {
  const currency = useSettingsStore((s) => s.currency);
  const setCurrency = useSettingsStore((s) => s.setCurrency);

  const select = useCallback(
    (value: CurrencyCode) => {
      setCurrency(value);
      const cur = useSettingsStore.getState();
      void SettingsStore.save({
        currency: cur.currency,
        pageSize: cur.pageSize,
        biometricEnabled: cur.biometricEnabled,
      });
      ToastService.success('Divisa actualizada', value);
    },
    [setCurrency],
  );

  return { currency, select };
}
