export type CurrencyCode = 'Bs' | '$' | '€';

export const CURRENCY_FRACTION_DIGITS = 2;

export interface Currency {
  readonly code: CurrencyCode;
  readonly symbol: string;
  readonly locale: string;
  readonly fractionDigits: number;
}

const BOLIVIAN_BOLIVAR: Currency = Object.freeze({
  code: 'Bs',
  symbol: 'Bs.',
  locale: 'es-BO',
  fractionDigits: CURRENCY_FRACTION_DIGITS,
});

const US_DOLLAR: Currency = Object.freeze({
  code: '$',
  symbol: '$',
  locale: 'en-US',
  fractionDigits: CURRENCY_FRACTION_DIGITS,
});

const EURO: Currency = Object.freeze({
  code: '€',
  symbol: '€',
  locale: 'es-ES',
  fractionDigits: CURRENCY_FRACTION_DIGITS,
});

export const CURRENCY_REGISTRY: ReadonlyMap<CurrencyCode, Currency> = new Map<
  CurrencyCode,
  Currency
>([
  ['Bs', BOLIVIAN_BOLIVAR],
  ['$', US_DOLLAR],
  ['€', EURO],
]);

export const CURRENCY_OPTIONS: ReadonlyArray<CurrencyCode> = Object.freeze([
  'Bs',
  '$',
  '€',
] as const);

export function getCurrency(code: CurrencyCode): Currency {
  const currency = CURRENCY_REGISTRY.get(code);
  if (currency === undefined) {
    throw new Error(`Unsupported currency: ${String(code)}`);
  }
  return currency;
}

export const CURRENCY_SYMBOLS: Readonly<Record<CurrencyCode, string>> = Object.freeze({
  Bs: BOLIVIAN_BOLIVAR.symbol,
  $: US_DOLLAR.symbol,
  '€': EURO.symbol,
});

export const CURRENCY_LOCALES: Readonly<Record<CurrencyCode, string>> = Object.freeze({
  Bs: BOLIVIAN_BOLIVAR.locale,
  $: US_DOLLAR.locale,
  '€': EURO.locale,
});
