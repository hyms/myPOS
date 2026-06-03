import { create } from 'zustand';
import { subscribeWithSelector } from 'zustand/middleware';

import type { CarritoItem, CarritoTipo } from '@/domain/entities/CarritoItem';
import { calcTotals } from '@/domain/rules/cartRules';
import type { ProductoResumen } from '@/domain/entities/Producto';

interface CarritoState {
  tipo: CarritoTipo;
  items: CarritoItem[];
  setTipo: (tipo: CarritoTipo) => void;
  agregar: (producto: ProductoResumen, cantidad?: number) => void;
  actualizar: (productoId: number, cantidad: number, descuento: number) => void;
  eliminar: (productoId: number) => void;
  vaciar: () => void;
  total: () => number;
  subtotal: () => number;
}

export const useCarritoStore = create<CarritoState>()(
  subscribeWithSelector((set, get) => ({
    tipo: 'VENTA',
    items: [],
    setTipo: (tipo) => set({ tipo, items: [] }),
    agregar: (producto, cantidad = 1) => {
      const items = get().items;
      const existing = items.find((it) => it.producto.id === producto.id);
      if (existing) {
        set({
          items: items.map((it) =>
            it.producto.id === producto.id ? { ...it, cantidad: it.cantidad + cantidad } : it,
          ),
        });
      } else {
        set({
          items: [...items, { producto, cantidad, descuento: 0 }],
        });
      }
    },
    actualizar: (productoId, cantidad, descuento) => {
      set({
        items: get().items.map((it) =>
          it.producto.id === productoId ? { ...it, cantidad, descuento } : it,
        ),
      });
    },
    eliminar: (productoId) => {
      set({ items: get().items.filter((it) => it.producto.id !== productoId) });
    },
    vaciar: () => set({ items: [] }),
    total: () => calcTotals(get().items, get().tipo).total,
    subtotal: () => calcTotals(get().items, get().tipo).subtotal,
  })),
);
