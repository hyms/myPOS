import AsyncStorage from '@react-native-async-storage/async-storage';

import type { CurrencyCode } from '@/domain/value-objects/Currency';

const STORAGE_KEY = 'tpv.settings.v1';

export interface PersistedSettings {
  readonly currency: CurrencyCode;
  readonly pageSize: number;
  readonly biometricEnabled: boolean;
}

const DEFAULTS: PersistedSettings = Object.freeze({
  currency: 'Bs',
  pageSize: 15,
  biometricEnabled: false,
});

export const SettingsStore = {
  async load(): Promise<PersistedSettings> {
    try {
      const raw = await AsyncStorage.getItem(STORAGE_KEY);
      if (!raw) return DEFAULTS;
      const parsed = JSON.parse(raw) as Partial<PersistedSettings>;
      return {
        currency: parsed.currency ?? DEFAULTS.currency,
        pageSize: parsed.pageSize ?? DEFAULTS.pageSize,
        biometricEnabled: parsed.biometricEnabled ?? DEFAULTS.biometricEnabled,
      };
    } catch {
      return DEFAULTS;
    }
  },

  async save(settings: PersistedSettings): Promise<void> {
    await AsyncStorage.setItem(STORAGE_KEY, JSON.stringify(settings));
  },

  defaults(): PersistedSettings {
    return DEFAULTS;
  },
};
