import { validateExpenseAmount, validateExpenseDescription } from '@/domain/rules/expenseRules';
import { Money } from '@/domain/value-objects/Money';

describe('validateExpenseAmount', () => {
  it('accepts a positive amount', () => {
    expect(() => validateExpenseAmount(new Money(25.5, 'Bs'))).not.toThrow();
  });

  it('rejects a zero amount', () => {
    expect(() => validateExpenseAmount(Money.zero('Bs'))).toThrow(
      'Expense amount must be greater than zero',
    );
  });

  it('rejects a negative amount', () => {
    expect(() => validateExpenseAmount(new Money(-5, '$'))).toThrow(
      'Expense amount must be greater than zero',
    );
  });
});

describe('validateExpenseDescription', () => {
  it('accepts a non-empty description', () => {
    expect(() => validateExpenseDescription('Luz del local')).not.toThrow();
  });

  it('rejects an empty description', () => {
    expect(() => validateExpenseDescription('')).toThrow('Expense description is required');
  });

  it('rejects a whitespace-only description', () => {
    expect(() => validateExpenseDescription('   ')).toThrow('Expense description is required');
  });
});
