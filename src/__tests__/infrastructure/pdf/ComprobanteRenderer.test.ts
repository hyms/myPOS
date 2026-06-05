import { buildComprobanteHTML } from '@/infrastructure/pdf/ComprobanteRenderer';
import type { CarritoItem } from '@/domain/entities/CarritoItem';

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

describe('buildComprobanteHTML', () => {
  it('genera HTML para VENTA con items', () => {
    const html = buildComprobanteHTML({
      tipo: 'VENTA',
      transaccionId: 1,
      items: [makeItem()],
      tipoPago: 'EFECTIVO',
      recibido: 500,
      cambio: 300,
      currency: 'Bs',
    });

    expect(html).toContain('COMPROBANTE DE VENTA');
    expect(html).toContain('#1');
    expect(html).toContain('Producto Test');
    expect(html).toContain('Bs.');
    expect(html).toContain('500');
    expect(html).toContain('300');
    expect(html).toContain('Gracias por su compra.');
  });

  it('genera HTML para COMPRA con items', () => {
    const html = buildComprobanteHTML({
      tipo: 'COMPRA',
      transaccionId: 2,
      items: [makeItem()],
      tipoPago: 'TARJETA',
      currency: '$',
    });

    expect(html).toContain('COMPROBANTE DE COMPRA');
    expect(html).toContain('#2');
    expect(html).toContain('$');
    expect(html).toContain('Compra registrada');
  });

  it('genera HTML para GASTO', () => {
    const html = buildComprobanteHTML({
      tipo: 'GASTO',
      transaccionId: 3,
      monto: 150.5,
      descripcion: 'Pago de servicios',
      tipoPago: 'TRANSFERENCIA',
      currency: 'Bs',
    });

    expect(html).toContain('COMPROBANTE DE GASTO');
    expect(html).toContain('Pago de servicios');
    expect(html).toContain('Bs.');
    expect(html).toContain('150.50');
    expect(html).toContain('Gasto registrado');
  });

  it('escapa HTML en descripciones', () => {
    const html = buildComprobanteHTML({
      tipo: 'GASTO',
      transaccionId: 4,
      monto: 100,
      descripcion: '<script>alert("xss")</script>',
      tipoPago: 'EFECTIVO',
      currency: 'Bs',
    });

    expect(html).not.toContain('<script>');
    expect(html).toContain('&lt;script&gt;');
  });

  it('no incluye recibido/cambio si no es VENTA', () => {
    const html = buildComprobanteHTML({
      tipo: 'COMPRA',
      transaccionId: 5,
      items: [makeItem()],
      tipoPago: 'EFECTIVO',
      currency: 'Bs',
    });

    expect(html).not.toContain('Recibido');
    expect(html).not.toContain('Cambio');
  });

  it('usa precioCompra para items de COMPRA', () => {
    const html = buildComprobanteHTML({
      tipo: 'COMPRA',
      transaccionId: 6,
      items: [makeItem()],
      tipoPago: 'EFECTIVO',
      currency: 'Bs',
    });

    // Para COMPRA, subtotal = 2 * 60 = 120
    expect(html).toContain('120');
  });
});
