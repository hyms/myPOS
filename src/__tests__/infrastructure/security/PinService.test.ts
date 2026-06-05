import { PinService } from '@/infrastructure/security/PinService';

import { resetSecureStore } from 'expo-secure-store';

describe('PinService', () => {
  beforeEach(() => {
    resetSecureStore();
  });

  describe('hasPin', () => {
    it('retorna false si no hay PIN configurado', async () => {
      expect(await PinService.hasPin()).toBe(false);
    });

    it('retorna true después de configurar PIN', async () => {
      await PinService.setPin('1234');
      expect(await PinService.hasPin()).toBe(true);
    });
  });

  describe('setPin', () => {
    it('lanza error si PIN tiene menos de 4 dígitos', async () => {
      await expect(PinService.setPin('123')).rejects.toThrow('4 dígitos');
    });

    it('almacena el PIN correctamente', async () => {
      await PinService.setPin('5678');
      expect(await PinService.verifyPin('5678')).toBe(true);
    });
  });

  describe('verifyPin', () => {
    it('retorna false si no hay PIN', async () => {
      expect(await PinService.verifyPin('1234')).toBe(false);
    });

    it('verifica PIN correcto', async () => {
      await PinService.setPin('9999');
      expect(await PinService.verifyPin('9999')).toBe(true);
    });

    it('rechaza PIN incorrecto', async () => {
      await PinService.setPin('1111');
      expect(await PinService.verifyPin('2222')).toBe(false);
    });

    it('retorna false si PIN < 4 caracteres', async () => {
      await PinService.setPin('1234');
      expect(await PinService.verifyPin('12')).toBe(false);
    });
  });

  describe('clearPin', () => {
    it('elimina el PIN configurado', async () => {
      await PinService.setPin('4321');
      await PinService.clearPin();
      expect(await PinService.hasPin()).toBe(false);
    });
  });

  describe('biometric', () => {
    it('por defecto está deshabilitado', async () => {
      expect(await PinService.isBiometricEnabled()).toBe(false);
    });

    it('habilita biometría', async () => {
      await PinService.setBiometricEnabled(true);
      expect(await PinService.isBiometricEnabled()).toBe(true);
    });

    it('deshabilita biometría', async () => {
      await PinService.setBiometricEnabled(true);
      await PinService.setBiometricEnabled(false);
      expect(await PinService.isBiometricEnabled()).toBe(false);
    });
  });
});
