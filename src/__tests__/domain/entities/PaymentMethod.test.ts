import type { PaymentMethodCode } from '@/domain/entities/PaymentMethod';
import {
  PAYMENT_METHOD_CODES,
  PaymentMethod,
  isPaymentMethodCode,
} from '@/domain/entities/PaymentMethod';

describe('PaymentMethod', () => {
  describe('PAYMENT_METHOD_CODES', () => {
    it('lists every supported payment method', () => {
      expect(PAYMENT_METHOD_CODES).toEqual(['CASH', 'CARD', 'TRANSFER', 'QR']);
    });

    it('is immutable', () => {
      expect(Object.isFrozen(PAYMENT_METHOD_CODES)).toBe(true);
    });
  });

  describe('constructor', () => {
    it('accepts every supported code', () => {
      for (const code of PAYMENT_METHOD_CODES) {
        expect(new PaymentMethod(code).code).toBe(code);
      }
    });

    it('throws for an unsupported code', () => {
      expect(() => new PaymentMethod('BITCOIN' as unknown as PaymentMethodCode)).toThrow(
        'Invalid payment method: BITCOIN',
      );
    });

    it('is case sensitive', () => {
      expect(() => new PaymentMethod('cash' as unknown as PaymentMethodCode)).toThrow(
        'Invalid payment method: cash',
      );
    });

    it('is immutable', () => {
      const method = new PaymentMethod('CASH');
      expect(Object.isFrozen(method)).toBe(true);
      expect(() => {
        (method as { code: PaymentMethodCode }).code = 'CARD';
      }).toThrow(TypeError);
    });
  });

  describe('isCash', () => {
    it('returns true only for the cash method', () => {
      expect(new PaymentMethod('CASH').isCash()).toBe(true);
      expect(new PaymentMethod('CARD').isCash()).toBe(false);
      expect(new PaymentMethod('TRANSFER').isCash()).toBe(false);
      expect(new PaymentMethod('QR').isCash()).toBe(false);
    });
  });

  describe('equals', () => {
    it('returns true for the same code', () => {
      expect(new PaymentMethod('QR').equals(new PaymentMethod('QR'))).toBe(true);
    });

    it('returns false for a different code', () => {
      expect(new PaymentMethod('QR').equals(new PaymentMethod('CARD'))).toBe(false);
    });
  });

  describe('toString', () => {
    it('renders the code', () => {
      expect(new PaymentMethod('TRANSFER').toString()).toBe('TRANSFER');
    });
  });
});

describe('isPaymentMethodCode', () => {
  it('returns true for every supported code', () => {
    for (const code of PAYMENT_METHOD_CODES) {
      expect(isPaymentMethodCode(code)).toBe(true);
    }
  });

  it('returns false for unsupported values', () => {
    expect(isPaymentMethodCode('BITCOIN')).toBe(false);
    expect(isPaymentMethodCode('cash')).toBe(false);
    expect(isPaymentMethodCode('')).toBe(false);
  });
});
