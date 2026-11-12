export type PaymentMethodCode = 'CASH' | 'CARD' | 'TRANSFER' | 'QR';

export const PAYMENT_METHOD_CODES: ReadonlyArray<PaymentMethodCode> = Object.freeze([
  'CASH',
  'CARD',
  'TRANSFER',
  'QR',
] as const);

const PAYMENT_METHOD_CODE_SET: ReadonlySet<string> = new Set<string>(PAYMENT_METHOD_CODES);

export function isPaymentMethodCode(value: string): value is PaymentMethodCode {
  return PAYMENT_METHOD_CODE_SET.has(value);
}

export class PaymentMethod {
  readonly code: PaymentMethodCode;

  constructor(code: PaymentMethodCode) {
    if (!isPaymentMethodCode(code)) {
      throw new Error(`Invalid payment method: ${String(code)}`);
    }

    this.code = code;

    Object.freeze(this);
  }

  equals(other: PaymentMethod): boolean {
    return this.code === other.code;
  }

  isCash(): boolean {
    return this.code === 'CASH';
  }

  toString(): string {
    return this.code;
  }
}
