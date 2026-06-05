import { useCallback, useEffect, useState } from 'react';

import { getRepositories } from '@/data/repositories/container';
import type { Categoria } from '@/domain/entities/Categoria';
import { useInvalidationStore } from '@/presentation/stores/invalidationStore';

export function useCategorias() {
  const [categorias, setCategorias] = useState<ReadonlyArray<Categoria>>(() =>
    getRepositories().categorias.listar(),
  );
  const invalidVersion = useInvalidationStore((s) => s.versions.categorias);

  useEffect(() => {
    setCategorias(getRepositories().categorias.listar());
  }, [invalidVersion]);

  const refresh = useCallback(() => {
    setCategorias(getRepositories().categorias.listar());
  }, []);

  return { categorias, refresh };
}
