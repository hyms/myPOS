import { TRANSACTION_TYPES, isTransactionType } from '@/domain/entities/TransactionType';

describe('TransactionType', () => {
  describe('TRANSACTION_TYPES', () => {
    it('lists every supported transaction type', () => {
      expect(TRANSACTION_TYPES).toEqual(['SALE', 'PURCHASE', 'EXPENSE']);
    });

    it('is immutable', () => {
      expect(Object.isFrozen(TRANSACTION_TYPES)).toBe(true);
    });
  });

  describe('isTransactionType', () => {
    it('returns true for every supported type', () => {
      for (const type of TRANSACTION_TYPES) {
        expect(isTransactionType(type)).toBe(true);
      }
    });

    it('returns false for unsupported values', () => {
      expect(isTransactionType('REFUND')).toBe(false);
      expect(isTransactionType('sale')).toBe(false);
      expect(isTransactionType('')).toBe(false);
    });
  });
});
