import type { CurrencyCode } from './Currency';
import { CURRENCY_FRACTION_DIGITS, getCurrency } from './Currency';

export const MONEY_FRACTION_DIGITS = CURRENCY_FRACTION_DIGITS;

const DECIMAL_SHIFT = 10 ** MONEY_FRACTION_DIGITS;

export function round2(value: number): number {
  if (!Number.isFinite(value)) {
    throw new Error('round2: value must be a finite number');
  }

  const scaled = Number(`${value}e${MONEY_FRACTION_DIGITS}`);

  if (!Number.isFinite(scaled)) {
    return Math.round(value * DECIMAL_SHIFT) / DECIMAL_SHIFT;
  }

  return Number(`${Math.round(scaled)}e-${MONEY_FRACTION_DIGITS}`);
}

export class Money {
  readonly amount: number;
  readonly currency: CurrencyCode;

  constructor(amount: number, currency: CurrencyCode) {
    if (!Number.isFinite(amount)) {
      throw new Error('Money: amount must be a finite number');
    }

    getCurrency(currency);

    const rounded = round2(amount);

    if (!Number.isFinite(rounded)) {
      throw new Error('Money: amount is out of the supported range');
    }

    this.amount = rounded;
    this.currency = currency;

    Object.freeze(this);
  }

  static zero(currency: CurrencyCode): Money {
    return new Money(0, currency);
  }

  add(other: Money): Money {
    this.assertSameCurrency(other, 'add');
    return new Money(this.amount + other.amount, this.currency);
  }

  subtract(other: Money): Money {
    this.assertSameCurrency(other, 'subtract');
    return new Money(this.amount - other.amount, this.currency);
  }

  multiply(scalar: number): Money {
    if (!Number.isFinite(scalar)) {
      throw new Error('Money.multiply: scalar must be a finite number');
    }
    return new Money(this.amount * scalar, this.currency);
  }

  divide(scalar: number): Money {
    if (!Number.isFinite(scalar)) {
      throw new Error('Money.divide: divisor must be a finite number');
    }
    if (scalar === 0) {
      throw new Error('Money.divide: divisor cannot be zero');
    }
    return new Money(this.amount / scalar, this.currency);
  }

  equals(other: Money): boolean {
    return this.currency === other.currency && this.amount === other.amount;
  }

  isZero(): boolean {
    return this.amount === 0;
  }

  isNegative(): boolean {
    return this.amount < 0;
  }

  isPositive(): boolean {
    return this.amount > 0;
  }

  format(locale?: string, fractionDigits?: number): string {
    const currency = getCurrency(this.currency);
    const formatter = new Intl.NumberFormat(locale ?? currency.locale, {
      minimumFractionDigits: fractionDigits ?? currency.fractionDigits,
      maximumFractionDigits: fractionDigits ?? currency.fractionDigits,
    });
    return `${currency.symbol} ${formatter.format(this.amount)}`;
  }

  toString(): string {
    return `${this.amount.toFixed(MONEY_FRACTION_DIGITS)} ${this.currency}`;
  }

  private assertSameCurrency(other: Money, operation: string): void {
    if (other.currency !== this.currency) {
      throw new Error(`Money.${operation}: currency mismatch`);
    }
  }
}
