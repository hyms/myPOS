import type { CartLine } from '@/domain/rules/cartRules';
import {
  applyDiscount,
  calcChange,
  calcTotals,
  lineSubtotal,
  selectUnitPrice,
} from '@/domain/rules/cartRules';
import { Money } from '@/domain/value-objects/Money';
import { Stock } from '@/domain/value-objects/Stock';

function makeLine(overrides?: Partial<CartLine>): CartLine {
  return {
    unitPrice: new Money(100, 'Bs'),
    quantity: new Stock(2),
    discount: Money.zero('Bs'),
    ...overrides,
  };
}

describe('selectUnitPrice', () => {
  it('returns the sale price for a sale cart', () => {
    const unitPrice = selectUnitPrice(new Money(100, 'Bs'), new Money(60, 'Bs'), 'SALE');
    expect(unitPrice.amount).toBe(100);
  });

  it('returns the purchase price for a purchase cart', () => {
    const unitPrice = selectUnitPrice(new Money(100, 'Bs'), new Money(60, 'Bs'), 'PURCHASE');
    expect(unitPrice.amount).toBe(60);
  });
});

describe('lineSubtotal', () => {
  it('multiplies the unit price by the quantity', () => {
    const subtotal = lineSubtotal(makeLine({ quantity: new Stock(3) }));
    expect(subtotal.amount).toBe(300);
    expect(subtotal.currency).toBe('Bs');
  });

  it('rounds the line result to two decimal places', () => {
    const subtotal = lineSubtotal(
      makeLine({ unitPrice: new Money(10.333, 'Bs'), quantity: new Stock(3) }),
    );
    expect(subtotal.amount).toBe(30.99);
  });

  it('returns zero for a zero quantity', () => {
    const subtotal = lineSubtotal(makeLine({ quantity: Stock.zero() }));
    expect(subtotal.amount).toBe(0);
  });
});

describe('calcTotals', () => {
  it('returns zero totals for an empty cart', () => {
    const totals = calcTotals([], '$');
    expect(totals.subtotal.amount).toBe(0);
    expect(totals.discount.amount).toBe(0);
    expect(totals.total.amount).toBe(0);
    expect(totals.total.currency).toBe('$');
  });

  it('sums every line subtotal', () => {
    const totals = calcTotals(
      [
        makeLine({ unitPrice: new Money(100, 'Bs'), quantity: new Stock(2) }),
        makeLine({ unitPrice: new Money(50, 'Bs'), quantity: new Stock(3) }),
      ],
      'Bs',
    );
    expect(totals.subtotal.amount).toBe(350);
    expect(totals.discount.amount).toBe(0);
    expect(totals.total.amount).toBe(350);
  });

  it('subtracts per-line discounts from the subtotal', () => {
    const totals = calcTotals(
      [makeLine({ quantity: new Stock(2), discount: new Money(10, 'Bs') })],
      'Bs',
    );
    expect(totals.subtotal.amount).toBe(200);
    expect(totals.discount.amount).toBe(10);
    expect(totals.total.amount).toBe(190);
  });

  it('never returns a negative total', () => {
    const totals = calcTotals(
      [makeLine({ quantity: new Stock(1), discount: new Money(500, 'Bs') })],
      'Bs',
    );
    expect(totals.total.amount).toBe(0);
  });

  it('accumulates decimal amounts without floating point drift', () => {
    const totals = calcTotals(
      [
        makeLine({ unitPrice: new Money(0.1, 'Bs'), quantity: new Stock(1) }),
        makeLine({ unitPrice: new Money(0.2, 'Bs'), quantity: new Stock(1) }),
      ],
      'Bs',
    );
    expect(totals.subtotal.amount).toBe(0.3);
  });
});

describe('applyDiscount', () => {
  it('subtracts a valid discount', () => {
    const result = applyDiscount(new Money(100, 'Bs'), new Money(20, 'Bs'));
    expect(result.amount).toBe(80);
  });

  it('ignores a negative discount', () => {
    const result = applyDiscount(new Money(100, 'Bs'), new Money(-10, 'Bs'));
    expect(result.amount).toBe(100);
  });

  it('returns zero when the discount exceeds the subtotal', () => {
    const result = applyDiscount(new Money(100, 'Bs'), new Money(200, 'Bs'));
    expect(result.amount).toBe(0);
  });

  it('returns zero when the discount equals the subtotal', () => {
    const result = applyDiscount(new Money(100, 'Bs'), new Money(100, 'Bs'));
    expect(result.amount).toBe(0);
  });

  it('throws on currency mismatch', () => {
    expect(() => applyDiscount(new Money(100, 'Bs'), new Money(20, '$'))).toThrow(
      'Money.subtract: currency mismatch',
    );
  });
});

describe('calcChange', () => {
  it('returns positive change when the received amount is higher', () => {
    const change = calcChange(new Money(100, 'Bs'), new Money(150, 'Bs'));
    expect(change.amount.amount).toBe(50);
    expect(change.isSufficient()).toBe(true);
    expect(change.getColorClass()).toBe('text-success');
  });

  it('returns zero change for an exact payment', () => {
    const change = calcChange(new Money(100, 'Bs'), new Money(100, 'Bs'));
    expect(change.amount.amount).toBe(0);
    expect(change.isSufficient()).toBe(true);
  });

  it('returns a negative amount when the received amount is lower', () => {
    const change = calcChange(new Money(100, 'Bs'), new Money(80, 'Bs'));
    expect(change.amount.amount).toBe(-20);
    expect(change.isSufficient()).toBe(false);
    expect(change.getColorClass()).toBe('text-danger');
  });

  it('rounds the change to two decimal places', () => {
    const change = calcChange(new Money(10.33, 'Bs'), new Money(20.67, 'Bs'));
    expect(change.amount.amount).toBe(10.34);
  });
});
