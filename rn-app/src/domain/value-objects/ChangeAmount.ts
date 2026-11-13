import type { CurrencyCode } from './Currency';
import { Money } from './Money';

export type ChangeColorClass = 'text-success' | 'text-danger';

export class ChangeAmount {
  readonly amount: Money;

  constructor(amount: Money) {
    this.amount = amount;

    Object.freeze(this);
  }

  static zero(currency: CurrencyCode): ChangeAmount {
    return new ChangeAmount(Money.zero(currency));
  }

  isSufficient(): boolean {
    return this.amount.amount >= 0;
  }

  getColorClass(): ChangeColorClass {
    return this.isSufficient() ? 'text-success' : 'text-danger';
  }

  equals(other: ChangeAmount): boolean {
    return this.amount.equals(other.amount);
  }

  toString(): string {
    return this.amount.toString();
  }
}
