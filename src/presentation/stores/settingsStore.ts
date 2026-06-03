import { create } from 'zustand';

import type { CurrencyCode } from '@/domain/value-objects/Currency';

interface SettingsState {
  currency: CurrencyCode;
  pageSize: number;
  biometricEnabled: boolean;
  hydrated: boolean;
  setCurrency: (c: CurrencyCode) => void;
  setPageSize: (n: number) => void;
  setBiometricEnabled: (b: boolean) => void;
  hydrate: (values: { currency: CurrencyCode; pageSize: number; biometricEnabled: boolean }) => void;
}

export const useSettingsStore = create<SettingsState>((set) => ({
  currency: 'Bs',
  pageSize: 15,
  biometricEnabled: false,
  hydrated: false,
  setCurrency: (currency) => set({ currency }),
  setPageSize: (pageSize) => set({ pageSize: Math.max(1, Math.min(100, Math.floor(pageSize))) }),
  setBiometricEnabled: (biometricEnabled) => set({ biometricEnabled }),
  hydrate: (values) => set({ ...values, hydrated: true }),
}));
