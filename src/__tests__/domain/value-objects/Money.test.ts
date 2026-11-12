import type { CurrencyCode } from '@/domain/value-objects/Currency';
import { Money, round2 } from '@/domain/value-objects/Money';

describe('Money', () => {
  describe('round2', () => {
    it('rounds half up to two decimal places', () => {
      expect(round2(1.005)).toBe(1.01);
      expect(round2(2.675)).toBe(2.68);
      expect(round2(1.004)).toBe(1);
      expect(round2(1.999)).toBe(2);
    });

    it('avoids binary floating point drift', () => {
      expect(round2(0.1 + 0.2)).toBe(0.3);
      expect(round2(1.15 * 100)).toBe(115);
    });

    it('rounds negative values toward positive infinity on ties', () => {
      expect(round2(-1.005)).toBe(-1);
      expect(round2(-1.006)).toBe(-1.01);
    });

    it('normalizes values below the precision to zero', () => {
      const result = round2(1e-7);
      expect(result).toBe(0);
      expect(Object.is(result, -0)).toBe(false);
    });

    it('throws for non-finite values', () => {
      expect(() => round2(Number.NaN)).toThrow('round2: value must be a finite number');
      expect(() => round2(Number.POSITIVE_INFINITY)).toThrow('finite');
      expect(() => round2(Number.NEGATIVE_INFINITY)).toThrow('finite');
    });
  });

  describe('constructor', () => {
    it('stores the amount and currency', () => {
      const money = new Money(100, 'Bs');
      expect(money.amount).toBe(100);
      expect(money.currency).toBe('Bs');
    });

    it('rounds the amount to two decimal places', () => {
      expect(new Money(10.005, '$').amount).toBe(10.01);
      expect(new Money(10.004, '$').amount).toBe(10);
      expect(new Money(0.1 + 0.2, '€').amount).toBe(0.3);
    });

    it('allows negative amounts for change calculations', () => {
      expect(new Money(-12.346, 'Bs').amount).toBe(-12.35);
      expect(new Money(-12.345, 'Bs').amount).toBe(-12.34);
    });

    it('throws for non-finite amounts', () => {
      expect(() => new Money(Number.NaN, 'Bs')).toThrow(
        'Money: amount must be a finite number',
      );
      expect(() => new Money(Number.POSITIVE_INFINITY, 'Bs')).toThrow('finite');
      expect(() => new Money(Number.NEGATIVE_INFINITY, 'Bs')).toThrow('finite');
    });

    it('throws for an unsupported currency code', () => {
      expect(() => new Money(10, 'USD' as unknown as CurrencyCode)).toThrow(
        'Unsupported currency: USD',
      );
    });

    it('is immutable', () => {
      const money = new Money(10, 'Bs');
      expect(Object.isFrozen(money)).toBe(true);
      expect(() => {
        (money as { amount: number }).amount = 99;
      }).toThrow(TypeError);
    });
  });

  describe('zero', () => {
    it('creates a zero amount in the given currency', () => {
      const money = Money.zero('$');
      expect(money.amount).toBe(0);
      expect(money.currency).toBe('$');
      expect(money.isZero()).toBe(true);
    });
  });

  describe('add', () => {
    it('adds amounts of the same currency', () => {
      const result = new Money(100, 'Bs').add(new Money(50.555, 'Bs'));
      expect(result.amount).toBe(150.56);
      expect(result.currency).toBe('Bs');
    });

    it('keeps the result rounded to two decimals', () => {
      expect(new Money(0.1, 'Bs').add(new Money(0.2, 'Bs')).amount).toBe(0.3);
    });

    it('throws on currency mismatch', () => {
      expect(() => new Money(100, 'Bs').add(new Money(50, '$'))).toThrow(
        'Money.add: currency mismatch',
      );
    });
  });

  describe('subtract', () => {
    it('subtracts amounts of the same currency', () => {
      const result = new Money(100, 'Bs').subtract(new Money(30.25, 'Bs'));
      expect(result.amount).toBe(69.75);
    });

    it('returns a negative amount when the subtrahend is larger', () => {
      const result = new Money(10, '$').subtract(new Money(25, '$'));
      expect(result.amount).toBe(-15);
      expect(result.isNegative()).toBe(true);
    });

    it('throws on currency mismatch', () => {
      expect(() => new Money(100, 'Bs').subtract(new Money(50, '€'))).toThrow(
        'Money.subtract: currency mismatch',
      );
    });
  });

  describe('multiply', () => {
    it('multiplies the amount by a scalar', () => {
      const result = new Money(19.99, 'Bs').multiply(3);
      expect(result.amount).toBe(59.97);
      expect(result.currency).toBe('Bs');
    });

    it('rounds the result to two decimals', () => {
      expect(new Money(10.005, '$').multiply(1).amount).toBe(10.01);
      expect(new Money(1, '€').multiply(1.005).amount).toBe(1.01);
    });

    it('supports zero and negative scalars', () => {
      expect(new Money(15, 'Bs').multiply(0).amount).toBe(0);
      expect(new Money(15, 'Bs').multiply(-2).amount).toBe(-30);
    });

    it('throws for a non-finite scalar', () => {
      expect(() => new Money(15, 'Bs').multiply(Number.NaN)).toThrow(
        'Money.multiply: scalar must be a finite number',
      );
    });
  });

  describe('divide', () => {
    it('divides the amount by a scalar', () => {
      const result = new Money(100, 'Bs').divide(4);
      expect(result.amount).toBe(25);
      expect(result.currency).toBe('Bs');
    });

    it('rounds the result to two decimals', () => {
      expect(new Money(100, '$').divide(3).amount).toBe(33.33);
    });

    it('supports negative scalars', () => {
      expect(new Money(15, '€').divide(-3).amount).toBe(-5);
    });

    it('throws for a zero divisor', () => {
      expect(() => new Money(15, 'Bs').divide(0)).toThrow(
        'Money.divide: divisor cannot be zero',
      );
    });

    it('throws for a non-finite divisor', () => {
      expect(() => new Money(15, 'Bs').divide(Number.NaN)).toThrow(
        'Money.divide: divisor must be a finite number',
      );
    });
  });

  describe('equals', () => {
    it('returns true for equal amount and currency', () => {
      expect(new Money(10, 'Bs').equals(new Money(10, 'Bs'))).toBe(true);
    });

    it('returns false for a different amount', () => {
      expect(new Money(10, 'Bs').equals(new Money(10.01, 'Bs'))).toBe(false);
    });

    it('returns false for a different currency', () => {
      expect(new Money(10, 'Bs').equals(new Money(10, '$'))).toBe(false);
    });
  });

  describe('sign helpers', () => {
    it('detects zero, positive and negative amounts', () => {
      const zero = new Money(0, 'Bs');
      const positive = new Money(0.01, 'Bs');
      const negative = new Money(-0.01, 'Bs');

      expect([zero.isZero(), positive.isZero(), negative.isZero()]).toEqual([
        true,
        false,
        false,
      ]);
      expect([zero.isPositive(), positive.isPositive(), negative.isPositive()]).toEqual([
        false,
        true,
        false,
      ]);
      expect([zero.isNegative(), positive.isNegative(), negative.isNegative()]).toEqual([
        false,
        false,
        true,
      ]);
    });
  });

  describe('format', () => {
    it('prefixes the currency symbol and uses two decimals', () => {
      expect(new Money(1234.5, '$').format('en-US')).toBe('$ 1,234.50');
    });

    it('uses the currency locale by default', () => {
      const formatted = new Money(1234.5, 'Bs').format();
      expect(formatted).toContain('Bs.');
      expect(formatted).toMatch(/1[.,]234[.,]50/);
    });

    it('formats the euro with its locale', () => {
      const formatted = new Money(99.9, '€').format();
      expect(formatted).toContain('€');
      expect(formatted).toMatch(/99[.,]90/);
    });

    it('accepts an explicit fraction digits override', () => {
      expect(new Money(5, '$').format('en-US', 0)).toBe('$ 5');
    });
  });

  describe('toString', () => {
    it('renders the fixed amount and currency code', () => {
      expect(new Money(5, 'Bs').toString()).toBe('5.00 Bs');
      expect(new Money(-5.5, '$').toString()).toBe('-5.50 $');
    });
  });
});
