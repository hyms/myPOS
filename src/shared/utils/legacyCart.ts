import type { CarritoItem, CarritoTipo } from '@/domain/entities/CarritoItem';
import type { CartKind, CartLine } from '@/domain/rules/cartRules';
import { selectUnitPrice } from '@/domain/rules/cartRules';
import type { CurrencyCode } from '@/domain/value-objects/Currency';
import { Money } from '@/domain/value-objects/Money';
import { Stock } from '@/domain/value-objects/Stock';

export function toCartKind(tipo: CarritoTipo): CartKind {
  return tipo === 'VENTA' ? 'SALE' : 'PURCHASE';
}

export function toCartLine(
  item: CarritoItem,
  kind: CartKind,
  currency: CurrencyCode,
): CartLine {
  return {
    unitPrice: selectUnitPrice(
      new Money(item.producto.precioVenta, currency),
      new Money(item.producto.precioCompra, currency),
      kind,
    ),
    quantity: new Stock(Math.max(0, item.cantidad)),
    discount: new Money(Math.max(0, item.descuento), currency),
  };
}

export function toCartLines(
  items: ReadonlyArray<CarritoItem>,
  kind: CartKind,
  currency: CurrencyCode,
): ReadonlyArray<CartLine> {
  return items.map((item) => toCartLine(item, kind, currency));
}
