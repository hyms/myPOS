import { useCallback, useEffect, useMemo, useState } from 'react';

import { getRepositories } from '@/data/repositories/container';
import type { Producto } from '@/domain/entities/Producto';
import type { SortOrder } from '@/data/repositories/IProductoRepository';
import { useSettingsStore } from '@/presentation/stores/settingsStore';

export interface UseProductosArgs {
  readonly search: string;
  readonly categoriaId?: number;
  readonly sort: SortOrder;
  readonly page: number;
}

export interface ProductoConPopularidad extends Producto {
  readonly popularidad: number;
}

export function useProductos({ search, categoriaId, sort, page }: UseProductosArgs) {
  const pageSize = useSettingsStore((s) => s.pageSize);
  const [items, setItems] = useState<ReadonlyArray<ProductoConPopularidad>>([]);
  const [loading, setLoading] = useState(true);

  const load = useCallback(() => {
    setLoading(true);
    const repo = getRepositories().productos;
    const list = repo.listar({
      search,
      categoriaId,
      sort,
      limit: pageSize,
      offset: page * pageSize,
    });
    setItems(list);
    setLoading(false);
  }, [search, categoriaId, sort, page, pageSize]);

  useEffect(() => {
    load();
  }, [load]);

  const refresh = useCallback(() => {
    load();
  }, [load]);

  const hasMore = useMemo(() => items.length === pageSize, [items.length, pageSize]);

  return { items, loading, hasMore, refresh };
}
