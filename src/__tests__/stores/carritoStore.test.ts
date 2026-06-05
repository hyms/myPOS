import { useVentaCarritoStore, useCompraCarritoStore } from '@/presentation/stores/carritoStore';
import type { ProductoResumen } from '@/domain/entities/Producto';

const producto: ProductoResumen = {
  id: 1,
  nombre: 'Coca Cola',
  precioVenta: 100,
  precioCompra: 60,
  stockActual: 10,
  imagenUri: null,
  categoriaId: 1,
};

const producto2: ProductoResumen = {
  id: 2,
  nombre: 'Fanta',
  precioVenta: 50,
  precioCompra: 30,
  stockActual: 5,
  imagenUri: null,
  categoriaId: 1,
};

describe('carritoStore (Venta)', () => {
  beforeEach(() => {
    useVentaCarritoStore.getState().vaciar();
  });

  it('inicia vacío', () => {
    expect(useVentaCarritoStore.getState().items).toEqual([]);
  });

  it('agrega un producto', () => {
    useVentaCarritoStore.getState().agregar(producto);
    const items = useVentaCarritoStore.getState().items;
    expect(items).toHaveLength(1);
    expect(items[0]!.producto.id).toBe(1);
    expect(items[0]!.cantidad).toBe(1);
  });

  it('incrementa cantidad si el producto ya existe', () => {
    useVentaCarritoStore.getState().agregar(producto, 2);
    useVentaCarritoStore.getState().agregar(producto, 3);
    const items = useVentaCarritoStore.getState().items;
    expect(items).toHaveLength(1);
    expect(items[0]!.cantidad).toBe(5);
  });

  it('actualiza cantidad y descuento', () => {
    useVentaCarritoStore.getState().agregar(producto);
    useVentaCarritoStore.getState().actualizar(1, 10, 15);
    const item = useVentaCarritoStore.getState().items[0]!;
    expect(item.cantidad).toBe(10);
    expect(item.descuento).toBe(15);
  });

  it('elimina un producto del carrito', () => {
    useVentaCarritoStore.getState().agregar(producto);
    useVentaCarritoStore.getState().agregar(producto2);
    useVentaCarritoStore.getState().eliminar(1);
    expect(useVentaCarritoStore.getState().items).toHaveLength(1);
    expect(useVentaCarritoStore.getState().items[0]!.producto.id).toBe(2);
  });

  it('vacia el carrito', () => {
    useVentaCarritoStore.getState().agregar(producto);
    useVentaCarritoStore.getState().agregar(producto2);
    useVentaCarritoStore.getState().vaciar();
    expect(useVentaCarritoStore.getState().items).toEqual([]);
  });
});

describe('carritoStore (Compra)', () => {
  beforeEach(() => {
    useCompraCarritoStore.getState().vaciar();
  });

  it('funciona independientemente de Venta', () => {
    useVentaCarritoStore.getState().agregar(producto);
    expect(useCompraCarritoStore.getState().items).toEqual([]);
  });
});
