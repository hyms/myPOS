import * as SecureStore from 'expo-secure-store';

const PIN_KEY = 'tpv.pin.hash';
const BIOMETRIC_KEY = 'tpv.biometric.enabled';

export const PinService = {
  async hasPin(): Promise<boolean> {
    const stored = await SecureStore.getItemAsync(PIN_KEY);
    return stored !== null && stored.length > 0;
  },

  async verifyPin(pin: string): Promise<boolean> {
    if (pin.length < 4) return false;
    const stored = await SecureStore.getItemAsync(PIN_KEY);
    if (!stored) return false;
    return stored === await hash(pin);
  },

  async setPin(pin: string): Promise<void> {
    if (pin.length < 4) {
      throw new Error('El PIN debe tener al menos 4 dígitos.');
    }
    const hashed = await hash(pin);
    await SecureStore.setItemAsync(PIN_KEY, hashed);
  },

  async clearPin(): Promise<void> {
    await SecureStore.deleteItemAsync(PIN_KEY);
  },

  async isBiometricEnabled(): Promise<boolean> {
    const v = await SecureStore.getItemAsync(BIOMETRIC_KEY);
    return v === '1';
  },

  async setBiometricEnabled(enabled: boolean): Promise<void> {
    await SecureStore.setItemAsync(BIOMETRIC_KEY, enabled ? '1' : '0');
  },
};

async function hash(input: string): Promise<string> {
  if (typeof globalThis.crypto?.subtle?.digest === 'function') {
    const enc = new TextEncoder().encode(input);
    const buffer = await globalThis.crypto.subtle.digest('SHA-256', enc);
    return Array.from(new Uint8Array(buffer))
      .map((b) => b.toString(16).padStart(2, '0'))
      .join('');
  }
  let h = 0;
  for (let i = 0; i < input.length; i++) {
    h = (h << 5) - h + input.charCodeAt(i);
    h |= 0;
  }
  return `fallback-${h.toString(16)}-${input.length}`;
}
