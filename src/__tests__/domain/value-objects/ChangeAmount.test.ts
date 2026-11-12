import { ChangeAmount } from '@/domain/value-objects/ChangeAmount';
import { Money } from '@/domain/value-objects/Money';

describe('ChangeAmount', () => {
  describe('constructor', () => {
    it('stores the wrapped money amount', () => {
      const money = new Money(5.5, 'Bs');
      expect(new ChangeAmount(money).amount).toBe(money);
    });

    it('is immutable', () => {
      const change = new ChangeAmount(new Money(1, 'Bs'));
      expect(Object.isFrozen(change)).toBe(true);
      expect(() => {
        (change as { amount: Money }).amount = new Money(2, 'Bs');
      }).toThrow(TypeError);
    });
  });

  describe('zero', () => {
    it('creates a zero change amount in the given currency', () => {
      const change = ChangeAmount.zero('$');
      expect(change.amount.amount).toBe(0);
      expect(change.amount.currency).toBe('$');
    });

    it('is sufficient because zero change is valid', () => {
      expect(ChangeAmount.zero('€').isSufficient()).toBe(true);
    });
  });

  describe('isSufficient', () => {
    it('returns true for a positive change', () => {
      expect(new ChangeAmount(new Money(12.5, 'Bs')).isSufficient()).toBe(true);
    });

    it('returns true for a zero change', () => {
      expect(new ChangeAmount(Money.zero('Bs')).isSufficient()).toBe(true);
    });

    it('returns false for a negative change', () => {
      expect(new ChangeAmount(new Money(-0.01, 'Bs')).isSufficient()).toBe(false);
    });
  });

  describe('getColorClass', () => {
    it('returns the success class for a sufficient change', () => {
      expect(new ChangeAmount(new Money(1, 'Bs')).getColorClass()).toBe('text-success');
      expect(new ChangeAmount(Money.zero('Bs')).getColorClass()).toBe('text-success');
    });

    it('returns the danger class for an insufficient change', () => {
      expect(new ChangeAmount(new Money(-1, 'Bs')).getColorClass()).toBe('text-danger');
    });
  });

  describe('equals', () => {
    it('returns true for equal amounts and currencies', () => {
      const left = new ChangeAmount(new Money(3.25, 'Bs'));
      const right = new ChangeAmount(new Money(3.25, 'Bs'));
      expect(left.equals(right)).toBe(true);
    });

    it('returns false for different amounts', () => {
      const left = new ChangeAmount(new Money(3.25, 'Bs'));
      const right = new ChangeAmount(new Money(3.26, 'Bs'));
      expect(left.equals(right)).toBe(false);
    });

    it('returns false for different currencies', () => {
      const left = new ChangeAmount(new Money(3.25, 'Bs'));
      const right = new ChangeAmount(new Money(3.25, '$'));
      expect(left.equals(right)).toBe(false);
    });
  });

  describe('toString', () => {
    it('delegates to the wrapped money representation', () => {
      expect(new ChangeAmount(new Money(2.5, 'Bs')).toString()).toBe('2.50 Bs');
    });
  });
});
