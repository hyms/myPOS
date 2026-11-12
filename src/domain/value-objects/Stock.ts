export const STOCK_FRACTION_DIGITS = 3;

const DECIMAL_SHIFT = 10 ** STOCK_FRACTION_DIGITS;

export function round3(value: number): number {
  if (!Number.isFinite(value)) {
    throw new Error('round3: value must be a finite number');
  }

  const scaled = Number(`${value}e${STOCK_FRACTION_DIGITS}`);

  if (!Number.isFinite(scaled)) {
    return Math.round(value * DECIMAL_SHIFT) / DECIMAL_SHIFT;
  }

  return Number(`${Math.round(scaled)}e-${STOCK_FRACTION_DIGITS}`);
}

export class Stock {
  readonly value: number;

  constructor(value: number) {
    if (!Number.isFinite(value)) {
      throw new Error('Stock: value must be a finite number');
    }

    if (value < 0) {
      throw new Error('Stock cannot be negative');
    }

    const rounded = round3(value);

    if (!Number.isFinite(rounded)) {
      throw new Error('Stock: value is out of the supported range');
    }

    this.value = rounded === 0 ? 0 : rounded;

    Object.freeze(this);
  }

  static zero(): Stock {
    return new Stock(0);
  }

  add(other: Stock): Stock {
    return new Stock(this.value + other.value);
  }

  subtract(other: Stock): Stock {
    const result = round3(this.value - other.value);

    if (result < 0) {
      throw new Error('Stock cannot go negative');
    }

    return new Stock(result);
  }

  isBelow(min: Stock): boolean {
    return this.value < min.value;
  }

  equals(other: Stock): boolean {
    return this.value === other.value;
  }

  toString(): string {
    return String(this.value);
  }
}
