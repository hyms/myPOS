import { BiometricService } from '@/infrastructure/security/BiometricService';

import { mockHardware, mockEnrolled, mockAuthSuccess } from 'expo-local-authentication';

describe('BiometricService', () => {
  beforeEach(() => {
    mockHardware(true);
    mockEnrolled(true);
    mockAuthSuccess(true);
  });

  describe('isAvailable', () => {
    it('retorna true si hay hardware y enrolled', async () => {
      expect(await BiometricService.isAvailable()).toBe(true);
    });

    it('retorna false si no hay hardware', async () => {
      mockHardware(false);
      expect(await BiometricService.isAvailable()).toBe(false);
    });

    it('retorna false si no hay enrolled', async () => {
      mockEnrolled(false);
      expect(await BiometricService.isAvailable()).toBe(false);
    });
  });

  describe('authenticate', () => {
    it('retorna true si autenticación exitosa', async () => {
      expect(await BiometricService.authenticate('Test')).toBe(true);
    });

    it('retorna false si falla', async () => {
      mockAuthSuccess(false);
      expect(await BiometricService.authenticate('Test')).toBe(false);
    });
  });
});
