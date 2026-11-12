export const SKU_MAX_LENGTH = 32;

const SKU_PATTERN = /^[A-Z0-9_-]+$/;

export class SKU {
  readonly value: string;

  constructor(value: string) {
    const normalized = value.trim().toUpperCase();

    if (normalized.length === 0) {
      throw new Error('SKU cannot be empty');
    }

    if (normalized.length > SKU_MAX_LENGTH) {
      throw new Error(`SKU max length is ${SKU_MAX_LENGTH}`);
    }

    if (!SKU_PATTERN.test(normalized)) {
      throw new Error('SKU must be alphanumeric with hyphen/underscore only');
    }

    this.value = normalized;

    Object.freeze(this);
  }

  equals(other: SKU): boolean {
    return this.value === other.value;
  }

  toString(): string {
    return this.value;
  }
}
