import { create } from 'zustand';

import type { Categoria } from '@/domain/entities/Categoria';
import type { Producto } from '@/domain/entities/Producto';

export type ProductoDraft = {
  categoriaId: number | null;
  nombre: string;
  precioVenta: string;
  precioCompra: string;
  stockActual: string;
  stockMinimo: string;
  imagenUri: string | null;
};

const EMPTY: ProductoDraft = {
  categoriaId: null,
  nombre: '',
  precioVenta: '',
  precioCompra: '',
  stockActual: '0',
  stockMinimo: '0',
  imagenUri: null,
};

interface ProductoDraftState {
  draft: ProductoDraft;
  editingId: number | null;
  categorias: Categoria[];
  setDraft: (patch: Partial<ProductoDraft>) => void;
  resetDraft: () => void;
  setCategorias: (c: ReadonlyArray<Categoria>) => void;
  startEdit: (p: Producto) => void;
  hydrateFromDraft: (d: ProductoDraft, categorias: ReadonlyArray<Categoria>, id: number | null) => void;
}

export const useProductoDraftStore = create<ProductoDraftState>((set) => ({
  draft: EMPTY,
  editingId: null,
  categorias: [],
  setDraft: (patch) => set((state) => ({ draft: { ...state.draft, ...patch } })),
  resetDraft: () => set({ draft: EMPTY, editingId: null }),
  setCategorias: (categorias) => set({ categorias: [...categorias] }),
  startEdit: (p) =>
    set({
      editingId: p.id,
      draft: {
        categoriaId: p.categoriaId,
        nombre: p.nombre,
        precioVenta: String(p.precioVenta),
        precioCompra: String(p.precioCompra),
        stockActual: String(p.stockActual),
        stockMinimo: String(p.stockMinimo),
        imagenUri: p.imagenUri,
      },
    }),
  hydrateFromDraft: (draft, categorias, id) =>
    set({ draft, categorias: [...categorias], editingId: id }),
}));

export const EMPTY_DRAFT = EMPTY;
