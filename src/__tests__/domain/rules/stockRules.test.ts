import { validateStockDisponible, isBelowMin, StockError } from '@/domain/rules/stockRules';

describe('validateStockDisponible', () => {
  const producto = { id: 1, nombre: 'Coca Cola', stockActual: 10 };

  it('lanza error si cantidad <= 0', () => {
    expect(() => validateStockDisponible(producto, 0)).toThrow(StockError);
    expect(() => validateStockDisponible(producto, -1)).toThrow(StockError);
  });

  it('lanza error si stock insuficiente', () => {
    expect(() => validateStockDisponible(producto, 11)).toThrow(StockError);
    expect(() => validateStockDisponible(producto, 100)).toThrow(StockError);
  });

  it('no lanza si hay stock suficiente', () => {
    expect(() => validateStockDisponible(producto, 10)).not.toThrow();
    expect(() => validateStockDisponible(producto, 1)).not.toThrow();
  });

  it('incluye nombre y stock en el mensaje de error', () => {
    try {
      validateStockDisponible(producto, 11);
    } catch (e) {
      const err = e as StockError;
      expect(err.message).toContain('Coca Cola');
      expect(err.message).toContain('10');
      expect(err.productoId).toBe(1);
      expect(err.productoNombre).toBe('Coca Cola');
    }
  });

  it('funciona con borde exacto (stockActual === cantidad)', () => {
    expect(() => validateStockDisponible(producto, 10)).not.toThrow();
  });
});

describe('isBelowMin', () => {
  it('retorna true si stockActual < stockMinimo', () => {
    expect(isBelowMin({ stockActual: 2, stockMinimo: 5 })).toBe(true);
  });

  it('retorna false si stockActual >= stockMinimo', () => {
    expect(isBelowMin({ stockActual: 5, stockMinimo: 5 })).toBe(false);
    expect(isBelowMin({ stockActual: 10, stockMinimo: 5 })).toBe(false);
  });

  it('retorna false si stockActual es 0 y stockMinimo es 0', () => {
    expect(isBelowMin({ stockActual: 0, stockMinimo: 0 })).toBe(false);
  });
});
