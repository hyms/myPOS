import type { TransactionType } from '../entities/TransactionType';
import { ChangeAmount } from '../value-objects/ChangeAmount';
import type { CurrencyCode } from '../value-objects/Currency';
import { Money } from '../value-objects/Money';
import type { Stock } from '../value-objects/Stock';

export type CartKind = Extract<TransactionType, 'SALE' | 'PURCHASE'>;

export interface CartLine {
  readonly unitPrice: Money;
  readonly quantity: Stock;
  readonly discount: Money;
}

export interface CartTotals {
  readonly subtotal: Money;
  readonly discount: Money;
  readonly total: Money;
}

export function selectUnitPrice(
  salePrice: Money,
  purchasePrice: Money,
  kind: CartKind,
): Money {
  return kind === 'SALE' ? salePrice : purchasePrice;
}

export function lineSubtotal(line: CartLine): Money {
  return line.unitPrice.multiply(line.quantity.value);
}

export function applyDiscount(subtotal: Money, discount: Money): Money {
  if (discount.isNegative()) return subtotal;
  if (discount.amount > subtotal.amount) return Money.zero(subtotal.currency);
  return subtotal.subtract(discount);
}

export function calcTotals(
  lines: ReadonlyArray<CartLine>,
  currency: CurrencyCode,
): CartTotals {
  let subtotal = Money.zero(currency);
  let discount = Money.zero(currency);

  for (const line of lines) {
    subtotal = subtotal.add(lineSubtotal(line));
    discount = discount.add(line.discount);
  }

  return {
    subtotal,
    discount,
    total: applyDiscount(subtotal, discount),
  };
}

export function calcChange(total: Money, received: Money): ChangeAmount {
  return new ChangeAmount(received.subtract(total));
}
