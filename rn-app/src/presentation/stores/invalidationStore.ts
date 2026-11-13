import { create } from 'zustand';

type EntityKey = 'productos' | 'transacciones' | 'caja' | 'categorias';

interface InvalidationState {
  readonly versions: Readonly<Record<EntityKey, number>>;
  invalidate: (key: EntityKey) => void;
  invalidateMany: (keys: ReadonlyArray<EntityKey>) => void;
}

export const useInvalidationStore = create<InvalidationState>((set) => ({
  versions: { productos: 0, transacciones: 0, caja: 0, categorias: 0 },
  invalidate: (key) =>
    set((s) => ({
      versions: { ...s.versions, [key]: s.versions[key] + 1 },
    })),
  invalidateMany: (keys) =>
    set((s) => {
      const next = { ...s.versions };
      for (const key of keys) {
        next[key] += 1;
      }
      return { versions: next };
    }),
}));
