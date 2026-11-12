import type { CurrencyCode } from '@/domain/value-objects/Currency';
import {
  CURRENCY_FRACTION_DIGITS,
  CURRENCY_LOCALES,
  CURRENCY_OPTIONS,
  CURRENCY_REGISTRY,
  CURRENCY_SYMBOLS,
  getCurrency,
} from '@/domain/value-objects/Currency';

describe('Currency', () => {
  describe('CURRENCY_REGISTRY', () => {
    it('registers exactly the supported currency codes', () => {
      expect([...CURRENCY_REGISTRY.keys()]).toEqual(['Bs', '$', '€']);
    });

    it('keeps the registry and its entries immutable', () => {
      expect(Object.isFrozen(CURRENCY_OPTIONS)).toBe(true);
      for (const currency of CURRENCY_REGISTRY.values()) {
        expect(Object.isFrozen(currency)).toBe(true);
      }
    });

    it('uses two fraction digits for every currency', () => {
      for (const currency of CURRENCY_REGISTRY.values()) {
        expect(currency.fractionDigits).toBe(CURRENCY_FRACTION_DIGITS);
      }
    });
  });

  describe('getCurrency', () => {
    it('returns the bolivian bolivar metadata', () => {
      expect(getCurrency('Bs')).toEqual({
        code: 'Bs',
        symbol: 'Bs.',
        locale: 'es-BO',
        fractionDigits: 2,
      });
    });

    it('returns the US dollar metadata', () => {
      expect(getCurrency('$')).toEqual({
        code: '$',
        symbol: '$',
        locale: 'en-US',
        fractionDigits: 2,
      });
    });

    it('returns the euro metadata', () => {
      expect(getCurrency('€')).toEqual({
        code: '€',
        symbol: '€',
        locale: 'es-ES',
        fractionDigits: 2,
      });
    });

    it('returns the same frozen instance on every lookup', () => {
      expect(getCurrency('Bs')).toBe(getCurrency('Bs'));
    });

    it('throws for an unsupported currency code', () => {
      expect(() => getCurrency('USD' as unknown as CurrencyCode)).toThrow(
        'Unsupported currency: USD',
      );
    });
  });

  describe('CURRENCY_OPTIONS', () => {
    it('lists every registered code in display order', () => {
      expect(CURRENCY_OPTIONS).toEqual(['Bs', '$', '€']);
    });
  });

  describe('legacy lookup maps', () => {
    it('derives symbols from the registry', () => {
      expect(CURRENCY_SYMBOLS).toEqual({ Bs: 'Bs.', $: '$', '€': '€' });
    });

    it('derives locales from the registry', () => {
      expect(CURRENCY_LOCALES).toEqual({ Bs: 'es-BO', $: 'en-US', '€': 'es-ES' });
    });

    it('covers every registered currency code', () => {
      for (const code of CURRENCY_REGISTRY.keys()) {
        expect(CURRENCY_SYMBOLS[code]).toBe(getCurrency(code).symbol);
        expect(CURRENCY_LOCALES[code]).toBe(getCurrency(code).locale);
      }
    });
  });
});
