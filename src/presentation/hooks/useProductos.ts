import { useCallback, useEffect, useRef, useState } from 'react';

import { getRepositories } from '@/data/repositories/container';
import type { Producto } from '@/domain/entities/Producto';
import type { SortOrder } from '@/data/repositories/IProductoRepository';
import { useSettingsStore } from '@/presentation/stores/settingsStore';
import { useInvalidationStore } from '@/presentation/stores/invalidationStore';

export interface UseProductosArgs {
  readonly search: string;
  readonly categoriaId?: number;
  readonly sort: SortOrder;
  readonly page: number;
}

export interface ProductoConPopularidad extends Producto {
  readonly popularidad: number;
}

function loadProductos(
  search: string,
  categoriaId: number | undefined,
  sort: SortOrder,
  page: number,
  pageSize: number,
): ReadonlyArray<ProductoConPopularidad> {
  return getRepositories().productos.listar({
    search,
    ...(categoriaId !== undefined ? { categoriaId } : {}),
    sort,
    limit: pageSize,
    offset: page * pageSize,
  });
}

export function useProductos({ search, categoriaId, sort, page }: UseProductosArgs) {
  const pageSize = useSettingsStore((s) => s.pageSize);
  const invalidVersion = useInvalidationStore((s) => s.versions.productos);
  const mounted = useRef(false);
  const [items, setItems] = useState<ReadonlyArray<ProductoConPopularidad>>(() =>
    loadProductos(search, categoriaId, sort, page, pageSize),
  );

  useEffect(() => {
    if (!mounted.current) {
      mounted.current = true;
      return;
    }
    setItems(loadProductos(search, categoriaId, sort, page, pageSize));
  }, [search, categoriaId, sort, page, pageSize, invalidVersion]);

  const refresh = useCallback(() => {
    setItems(loadProductos(search, categoriaId, sort, page, pageSize));
  }, [search, categoriaId, sort, page, pageSize]);

  return {
    items,
    loading: false,
    hasMore: items.length === pageSize,
    refresh,
  };
}
