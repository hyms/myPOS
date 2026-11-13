import { assertStockAvailable, isBelowMin, StockError } from '@/domain/rules/stockRules';
import { Stock } from '@/domain/value-objects/Stock';

function makeProduct(currentStock = 10) {
  return { id: 1, name: 'Coca Cola', currentStock: new Stock(currentStock) };
}

describe('assertStockAvailable', () => {
  it('throws for a non-positive requested quantity', () => {
    expect(() => assertStockAvailable(makeProduct(), Stock.zero())).toThrow(StockError);
  });

  it('throws when the requested quantity exceeds the available stock', () => {
    expect(() => assertStockAvailable(makeProduct(), new Stock(11))).toThrow(StockError);
    expect(() => assertStockAvailable(makeProduct(), new Stock(100))).toThrow(StockError);
  });

  it('does not throw when the stock is sufficient', () => {
    expect(() => assertStockAvailable(makeProduct(), new Stock(10))).not.toThrow();
    expect(() => assertStockAvailable(makeProduct(), new Stock(1))).not.toThrow();
  });

  it('accepts the exact stock boundary', () => {
    expect(() => assertStockAvailable(makeProduct(10), new Stock(10))).not.toThrow();
  });

  it('exposes structured fields for presentation localization', () => {
    try {
      assertStockAvailable(makeProduct(), new Stock(11));
      throw new Error('Expected assertStockAvailable to throw');
    } catch (error) {
      const stockError = error as StockError;
      expect(stockError).toBeInstanceOf(StockError);
      expect(stockError.reason).toBe('INSUFFICIENT_STOCK');
      expect(stockError.productId).toBe(1);
      expect(stockError.productName).toBe('Coca Cola');
      expect(stockError.available.value).toBe(10);
      expect(stockError.requested.value).toBe(11);
      expect(stockError.message).toContain('Coca Cola');
      expect(stockError.message).toContain('10');
    }
  });

  it('reports the non-positive quantity reason', () => {
    try {
      assertStockAvailable(makeProduct(), Stock.zero());
      throw new Error('Expected assertStockAvailable to throw');
    } catch (error) {
      const stockError = error as StockError;
      expect(stockError.reason).toBe('NON_POSITIVE_QUANTITY');
      expect(stockError.requested.value).toBe(0);
    }
  });
});

describe('isBelowMin', () => {
  it('returns true when the current stock is below the minimum', () => {
    expect(isBelowMin(new Stock(2), new Stock(5))).toBe(true);
  });

  it('returns false when the current stock is at or above the minimum', () => {
    expect(isBelowMin(new Stock(5), new Stock(5))).toBe(false);
    expect(isBelowMin(new Stock(10), new Stock(5))).toBe(false);
  });

  it('returns false when both values are zero', () => {
    expect(isBelowMin(Stock.zero(), Stock.zero())).toBe(false);
  });
});
