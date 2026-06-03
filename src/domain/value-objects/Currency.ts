export type CurrencyCode = 'Bs' | '$' | '€';

export const CURRENCY_SYMBOLS: Readonly<Record<CurrencyCode, string>> = Object.freeze({
  Bs: 'Bs.',
  $: '$',
  '€': '€',
});

export const CURRENCY_LOCALES: Readonly<Record<CurrencyCode, string>> = Object.freeze({
  Bs: 'es-BO',
  $: 'en-US',
  '€': 'es-ES',
});

export const CURRENCY_OPTIONS: ReadonlyArray<CurrencyCode> = ['Bs', '$', '€'];
