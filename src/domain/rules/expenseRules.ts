import type { Money } from '../value-objects/Money';

export function validateExpenseAmount(amount: Money): void {
  if (!amount.isPositive()) {
    throw new Error('Expense amount must be greater than zero');
  }
}

export function validateExpenseDescription(description: string): void {
  if (description.trim().length === 0) {
    throw new Error('Expense description is required');
  }
}
