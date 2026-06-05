import { create } from 'zustand';

import type { CarritoTipo } from '@/domain/entities/CarritoItem';

interface RegistroSubtabState {
  readonly tipo: CarritoTipo;
  readonly setTipo: (tipo: CarritoTipo) => void;
}

export const useRegistroSubtabStore = create<RegistroSubtabState>((set) => ({
  tipo: 'VENTA',
  setTipo: (tipo) => set({ tipo }),
}));
