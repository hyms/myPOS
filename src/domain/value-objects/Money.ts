import type { CurrencyCode } from './Currency';
import { CURRENCY_LOCALES, CURRENCY_SYMBOLS } from './Currency';

export class Money {
  readonly amount: number;
  readonly currency: CurrencyCode;

  constructor(amount: number, currency: CurrencyCode) {
    if (!Number.isFinite(amount)) {
      throw new Error('Money: amount must be a finite number');
    }
    this.amount = amount;
    this.currency = currency;
  }

  static zero(currency: CurrencyCode): Money {
    return new Money(0, currency);
  }

  format(locale?: string, fractionDigits = 2): string {
    const safeLocale = locale ?? CURRENCY_LOCALES[this.currency];
    const formatter = new Intl.NumberFormat(safeLocale, {
      minimumFractionDigits: fractionDigits,
      maximumFractionDigits: fractionDigits,
    });
    const symbol = CURRENCY_SYMBOLS[this.currency];
    return `${symbol} ${formatter.format(this.amount)}`;
  }

  add(other: Money): Money {
    if (other.currency !== this.currency) {
      throw new Error('Money.add: currency mismatch');
    }
    return new Money(this.amount + other.amount, this.currency);
  }

  subtract(other: Money): Money {
    if (other.currency !== this.currency) {
      throw new Error('Money.subtract: currency mismatch');
    }
    return new Money(this.amount - other.amount, this.currency);
  }
}
