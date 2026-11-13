import { create, type StoreApi, type UseBoundStore } from 'zustand';
import { subscribeWithSelector } from 'zustand/middleware';

import type { CarritoItem } from '@/domain/entities/CarritoItem';
import type { ProductoResumen } from '@/domain/entities/Producto';

export interface CarritoState {
  items: CarritoItem[];
  agregar: (producto: ProductoResumen, cantidad?: number) => void;
  actualizar: (productoId: number, cantidad: number, descuento: number) => void;
  eliminar: (productoId: number) => void;
  vaciar: () => void;
}

export type CarritoStoreHook = UseBoundStore<StoreApi<CarritoState>>;

const createCarritoStore = (): CarritoStoreHook =>
  create<CarritoState>()(
    subscribeWithSelector((set, get) => ({
      items: [],
      agregar: (producto, cantidad = 1) => {
        const items = get().items;
        const existing = items.find((it) => it.producto.id === producto.id);
        if (existing) {
          set({
            items: items.map((it) =>
              it.producto.id === producto.id
                ? { ...it, cantidad: it.cantidad + cantidad }
                : it,
            ),
          });
        } else {
          set({ items: [...items, { producto, cantidad, descuento: 0 }] });
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
    })),
  );

export const useVentaCarritoStore = createCarritoStore();
export const useCompraCarritoStore = createCarritoStore();
