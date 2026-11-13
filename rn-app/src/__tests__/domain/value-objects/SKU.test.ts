import { SKU, SKU_MAX_LENGTH } from '@/domain/value-objects/SKU';

describe('SKU', () => {
  describe('constructor', () => {
    it('stores an uppercase alphanumeric value', () => {
      expect(new SKU('ABC123').value).toBe('ABC123');
    });

    it('trims whitespace and normalizes to uppercase', () => {
      expect(new SKU('  abc-1_2  ').value).toBe('ABC-1_2');
    });

    it('accepts hyphens and underscores', () => {
      expect(new SKU('SKU-001_A').value).toBe('SKU-001_A');
    });

    it('accepts numeric-only values', () => {
      expect(new SKU('12345').value).toBe('12345');
    });

    it('accepts a value with the maximum length', () => {
      const value = 'A'.repeat(SKU_MAX_LENGTH);
      expect(new SKU(value).value).toBe(value);
    });

    it('throws for an empty or whitespace-only value', () => {
      expect(() => new SKU('')).toThrow('SKU cannot be empty');
      expect(() => new SKU('   ')).toThrow('SKU cannot be empty');
    });

    it('throws when the value exceeds the maximum length', () => {
      expect(() => new SKU('A'.repeat(SKU_MAX_LENGTH + 1))).toThrow(
        `SKU max length is ${SKU_MAX_LENGTH}`,
      );
    });

    it('throws for characters outside the allowed pattern', () => {
      expect(() => new SKU('AB C')).toThrow(
        'SKU must be alphanumeric with hyphen/underscore only',
      );
      expect(() => new SKU('AB@C')).toThrow('SKU must be alphanumeric');
      expect(() => new SKU('ABC.')).toThrow('SKU must be alphanumeric');
      expect(() => new SKU('ÁBC')).toThrow('SKU must be alphanumeric');
    });

    it('measures the maximum length after normalization', () => {
      expect(() => new SKU(`  ${'A'.repeat(SKU_MAX_LENGTH)}  `)).not.toThrow();
    });

    it('is immutable', () => {
      const sku = new SKU('ABC');
      expect(Object.isFrozen(sku)).toBe(true);
      expect(() => {
        (sku as { value: string }).value = 'XYZ';
      }).toThrow(TypeError);
    });
  });

  describe('equals', () => {
    it('returns true for equivalent normalized values', () => {
      expect(new SKU('abc-1').equals(new SKU('  ABC-1 '))).toBe(true);
    });

    it('returns false for different values', () => {
      expect(new SKU('ABC-1').equals(new SKU('ABC-2'))).toBe(false);
    });
  });

  describe('toString', () => {
    it('renders the normalized value', () => {
      expect(new SKU('  abc_1 ').toString()).toBe('ABC_1');
    });
  });
});
