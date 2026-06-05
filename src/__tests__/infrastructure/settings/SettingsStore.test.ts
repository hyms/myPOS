import AsyncStorage from '@react-native-async-storage/async-storage';

import { SettingsStore } from '@/infrastructure/settings/SettingsStore';

import { resetAsyncStorage } from '@react-native-async-storage/async-storage';

describe('SettingsStore', () => {
  beforeEach(() => {
    resetAsyncStorage();
  });

  describe('load', () => {
    it('retorna valores por defecto si no hay datos', async () => {
      const s = await SettingsStore.load();
      expect(s.currency).toBe('Bs');
      expect(s.pageSize).toBe(15);
      expect(s.biometricEnabled).toBe(false);
    });

    it('retorna valores guardados', async () => {
      await SettingsStore.save({
        currency: '$',
        pageSize: 20,
        biometricEnabled: true,
      });
      const s = await SettingsStore.load();
      expect(s.currency).toBe('$');
      expect(s.pageSize).toBe(20);
      expect(s.biometricEnabled).toBe(true);
    });

    it('tolera JSON corrupto y retorna defaults', async () => {
      await AsyncStorage.setItem('tpv.settings.v1', 'not-json');
      const s = await SettingsStore.load();
      expect(s.currency).toBe('Bs');
    });
  });

  describe('save', () => {
    it('persiste settings en AsyncStorage', async () => {
      await SettingsStore.save({
        currency: '€',
        pageSize: 30,
        biometricEnabled: false,
      });
      const raw = await AsyncStorage.getItem('tpv.settings.v1');
      const parsed = JSON.parse(raw!);
      expect(parsed.currency).toBe('€');
      expect(parsed.pageSize).toBe(30);
    });
  });
});
