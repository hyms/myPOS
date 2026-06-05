import type { CarritoItem } from '@/domain/entities/CarritoItem';
import { getUnitPrice, lineSubtotal, calcTotals, calcChange, applyDiscount } from '@/domain/rules/cartRules';

function makeItem(overrides?: Partial<CarritoItem>): CarritoItem {
  return {
    producto: {
      id: 1,
      nombre: 'Producto Test',
      precioVenta: 100,
      precioCompra: 60,
      stockActual: 10,
      imagenUri: null,
    },
    cantidad: 2,
    descuento: 0,
    ...overrides,
  };
}

describe('getUnitPrice', () => {
  it('usa precioVenta para VENTA', () => {
    expect(getUnitPrice(makeItem(), 'VENTA')).toBe(100);
  });

  it('usa precioCompra para COMPRA', () => {
    expect(getUnitPrice(makeItem(), 'COMPRA')).toBe(60);
  });
});

describe('lineSubtotal', () => {
  it('calcula subtotal por línea en VENTA', () => {
    expect(lineSubtotal(makeItem({ cantidad: 3 }), 'VENTA')).toBe(300);
  });

  it('calcula subtotal por línea en COMPRA', () => {
    expect(lineSubtotal(makeItem({ cantidad: 3 }), 'COMPRA')).toBe(180);
  });
});

describe('calcTotals', () => {
  it('calcula totales vacíos', () => {
    const t = calcTotals([], 'VENTA');
    expect(t.subtotal).toBe(0);
    expect(t.descuento).toBe(0);
    expect(t.total).toBe(0);
  });

  it('suma correctamente los items', () => {
    const items = [
      makeItem({ producto: { id: 1, nombre: 'A', precioVenta: 100, precioCompra: 60, stockActual: 10, imagenUri: null }, cantidad: 2 }),
      makeItem({ producto: { id: 2, nombre: 'B', precioVenta: 50, precioCompra: 30, stockActual: 5, imagenUri: null }, cantidad: 3 }),
    ];
    const t = calcTotals(items, 'VENTA');
    expect(t.subtotal).toBe(350); // 200 + 150
    expect(t.total).toBe(350);
  });

  it('aplica descuentos globales', () => {
    const items = [
      makeItem({ cantidad: 2, descuento: 10 }),
    ];
    const t = calcTotals(items, 'VENTA');
    expect(t.subtotal).toBe(200);
    expect(t.descuento).toBe(10);
    expect(t.total).toBe(190);
  });

  it('redondea a 2 decimales', () => {
    const items = [
      makeItem({ producto: { id: 1, nombre: 'A', precioVenta: 10.333, precioCompra: 5, stockActual: 10, imagenUri: null }, cantidad: 3 }),
    ];
    const t = calcTotals(items, 'VENTA');
    expect(t.subtotal).toBe(31.0);
    expect(t.total).toBe(31.0);
  });
});

describe('calcChange', () => {
  it('calcula cambio positivo', () => {
    expect(calcChange(100, 150)).toBe(50);
  });

  it('retorna 0 si pago exacto', () => {
    expect(calcChange(100, 100)).toBe(0);
  });

  it('retorna negativo si falta dinero', () => {
    expect(calcChange(100, 80)).toBe(-20);
  });

  it('redondea a 2 decimales', () => {
    expect(calcChange(10.333, 20.666)).toBe(10.33);
  });
});

describe('applyDiscount', () => {
  it('aplica descuento válido', () => {
    expect(applyDiscount(100, 20)).toBe(80);
  });

  it('no permite descuento negativo (retorna subtotal)', () => {
    expect(applyDiscount(100, -10)).toBe(100);
  });

  it('no permite total negativo (retorna 0)', () => {
    expect(applyDiscount(100, 200)).toBe(0);
  });

  it('descuento exacto igual al subtotal', () => {
    expect(applyDiscount(100, 100)).toBe(0);
  });
});
