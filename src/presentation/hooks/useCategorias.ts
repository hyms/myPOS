import { useCallback, useEffect, useState } from 'react';

import { getRepositories } from '@/data/repositories/container';
import type { Categoria } from '@/domain/entities/Categoria';

export function useCategorias() {
  const [categorias, setCategorias] = useState<ReadonlyArray<Categoria>>([]);

  const refresh = useCallback(() => {
    setCategorias(getRepositories().categorias.listar());
  }, []);

  useEffect(() => {
    refresh();
  }, [refresh]);

  return { categorias, refresh };
}
