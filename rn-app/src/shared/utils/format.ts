import type { CurrencyCode } from '@/domain/value-objects/Currency';
import { CURRENCY_LOCALES, CURRENCY_SYMBOLS } from '@/domain/value-objects/Currency';

export function formatCurrency(amount: number, currency: CurrencyCode, fractionDigits = 2): string {
  const locale = CURRENCY_LOCALES[currency];
  const formatter = new Intl.NumberFormat(locale, {
    minimumFractionDigits: fractionDigits,
    maximumFractionDigits: fractionDigits,
  });
  return `${CURRENCY_SYMBOLS[currency]} ${formatter.format(amount)}`;
}

export function parseIntStrict(value: string): number {
  const n = Number.parseInt(value, 10);
  return Number.isFinite(n) ? n : 0;
}

export function parseFloatStrict(value: string): number {
  const n = Number.parseFloat(value);
  return Number.isFinite(n) ? n : 0;
}
