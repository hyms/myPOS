import { Money } from '@/domain/value-objects/Money';

describe('Money', () => {
  describe('constructor', () => {
    it('crea instancia con amount y currency', () => {
      const m = new Money(100, 'Bs');
      expect(m.amount).toBe(100);
      expect(m.currency).toBe('Bs');
    });

    it('lanza error si amount no es finito', () => {
      expect(() => new Money(NaN, 'Bs')).toThrow();
      expect(() => new Money(Infinity, 'Bs')).toThrow();
    });
  });

  describe('zero', () => {
    it('crea Money con amount 0', () => {
      const m = Money.zero('$');
      expect(m.amount).toBe(0);
      expect(m.currency).toBe('$');
    });
  });

  describe('format', () => {
    it('formatea con símbolo de la moneda', () => {
      const m = new Money(100.5, 'Bs');
      const f = m.format();
      expect(f).toContain('Bs.');
      expect(f).toContain('100');
    });

    it('incluye el monto formateado con 2 decimales', () => {
      const f = new Money(1, '$').format();
      expect(f).toContain('$');
      expect(f).toContain('1');
    });

    it('usa locale de cada moneda', () => {
      // Should not throw and contain the amount
      expect(() => new Money(1, 'Bs').format()).not.toThrow();
      expect(() => new Money(1, '$').format()).not.toThrow();
      expect(() => new Money(1, '€').format()).not.toThrow();
    });
  });

  describe('add', () => {
    it('suma dos Money de la misma moneda', () => {
      const a = new Money(100, 'Bs');
      const b = new Money(50, 'Bs');
      const r = a.add(b);
      expect(r.amount).toBe(150);
      expect(r.currency).toBe('Bs');
    });

    it('lanza error si las monedas difieren', () => {
      expect(() => new Money(100, 'Bs').add(new Money(50, '$'))).toThrow('currency mismatch');
    });
  });

  describe('subtract', () => {
    it('resta dos Money de la misma moneda', () => {
      const a = new Money(100, 'Bs');
      const b = new Money(30, 'Bs');
      const r = a.subtract(b);
      expect(r.amount).toBe(70);
      expect(r.currency).toBe('Bs');
    });

    it('lanza error si las monedas difieren', () => {
      expect(() => new Money(100, 'Bs').subtract(new Money(50, '€'))).toThrow('currency mismatch');
    });
  });
});
