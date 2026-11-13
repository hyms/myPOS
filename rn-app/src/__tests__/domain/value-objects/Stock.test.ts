import { Stock, round3 } from '@/domain/value-objects/Stock';

describe('Stock', () => {
  describe('round3', () => {
    it('rounds half up to three decimal places', () => {
      expect(round3(1.2345)).toBe(1.235);
      expect(round3(1.2344)).toBe(1.234);
      expect(round3(0.0005)).toBe(0.001);
    });

    it('avoids binary floating point drift', () => {
      expect(round3(0.1 + 0.2)).toBe(0.3);
      expect(round3(0.003 - 0.002)).toBe(0.001);
    });

    it('normalizes values below the precision to zero', () => {
      const result = round3(1e-9);
      expect(result).toBe(0);
      expect(Object.is(result, -0)).toBe(false);
    });

    it('throws for non-finite values', () => {
      expect(() => round3(Number.NaN)).toThrow('round3: value must be a finite number');
      expect(() => round3(Number.POSITIVE_INFINITY)).toThrow('finite');
    });
  });

  describe('constructor', () => {
    it('stores a non-negative integer value', () => {
      expect(new Stock(12).value).toBe(12);
    });

    it('stores fractional values up to three decimal places', () => {
      expect(new Stock(1.25).value).toBe(1.25);
      expect(new Stock(0.125).value).toBe(0.125);
    });

    it('rounds the value to three decimal places', () => {
      expect(new Stock(1.2345).value).toBe(1.235);
      expect(new Stock(1.2344).value).toBe(1.234);
      expect(new Stock(0.0005).value).toBe(0.001);
    });

    it('normalizes values below the precision to zero', () => {
      const stock = new Stock(0.0004);
      expect(stock.value).toBe(0);
      expect(Object.is(stock.value, -0)).toBe(false);
    });

    it('accepts zero', () => {
      expect(new Stock(0).value).toBe(0);
    });

    it('throws for negative values', () => {
      expect(() => new Stock(-1)).toThrow('Stock cannot be negative');
      expect(() => new Stock(-0.001)).toThrow('Stock cannot be negative');
      expect(() => new Stock(-0.0004)).toThrow('Stock cannot be negative');
    });

    it('throws for non-finite values', () => {
      expect(() => new Stock(Number.NaN)).toThrow('Stock: value must be a finite number');
      expect(() => new Stock(Number.POSITIVE_INFINITY)).toThrow('finite');
      expect(() => new Stock(Number.NEGATIVE_INFINITY)).toThrow('finite');
    });

    it('is immutable', () => {
      const stock = new Stock(5);
      expect(Object.isFrozen(stock)).toBe(true);
      expect(() => {
        (stock as { value: number }).value = 99;
      }).toThrow(TypeError);
    });
  });

  describe('zero', () => {
    it('creates a zero stock', () => {
      const stock = Stock.zero();
      expect(stock.value).toBe(0);
      expect(stock.equals(new Stock(0))).toBe(true);
    });
  });

  describe('add', () => {
    it('adds two stock values', () => {
      expect(new Stock(1.5).add(new Stock(2.25)).value).toBe(3.75);
    });

    it('keeps the result rounded to three decimals', () => {
      expect(new Stock(0.1).add(new Stock(0.2)).value).toBe(0.3);
    });

    it('returns a new instance without mutating the operands', () => {
      const left = new Stock(1);
      const right = new Stock(2);
      const result = left.add(right);

      expect(result).not.toBe(left);
      expect(left.value).toBe(1);
      expect(right.value).toBe(2);
    });
  });

  describe('subtract', () => {
    it('subtracts two stock values', () => {
      expect(new Stock(5).subtract(new Stock(2.5)).value).toBe(2.5);
    });

    it('returns zero when both values are equal', () => {
      expect(new Stock(3.125).subtract(new Stock(3.125)).value).toBe(0);
    });

    it('preserves three decimal precision', () => {
      expect(new Stock(0.003).subtract(new Stock(0.002)).value).toBe(0.001);
    });

    it('throws when the result would be negative', () => {
      expect(() => new Stock(1).subtract(new Stock(1.001))).toThrow(
        'Stock cannot go negative',
      );
    });
  });

  describe('isBelow', () => {
    it('returns true when the stock is lower than the minimum', () => {
      expect(new Stock(1.999).isBelow(new Stock(2))).toBe(true);
    });

    it('returns false when the stock equals the minimum', () => {
      expect(new Stock(2).isBelow(new Stock(2))).toBe(false);
    });

    it('returns false when the stock is above the minimum', () => {
      expect(new Stock(2.001).isBelow(new Stock(2))).toBe(false);
    });
  });

  describe('equals', () => {
    it('returns true for equal values', () => {
      expect(new Stock(1.25).equals(new Stock(1.25))).toBe(true);
    });

    it('returns false for different values', () => {
      expect(new Stock(1.25).equals(new Stock(1.251))).toBe(false);
    });
  });

  describe('toString', () => {
    it('renders the numeric value', () => {
      expect(new Stock(1.5).toString()).toBe('1.5');
      expect(new Stock(0).toString()).toBe('0');
    });
  });
});
