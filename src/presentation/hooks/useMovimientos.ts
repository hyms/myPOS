import { useCallback, useEffect, useMemo, useState } from 'react';

import { getRepositories } from '@/data/repositories/container';
import type { Transaccion, TipoTransaccion } from '@/domain/entities/Transaccion';
import { useSettingsStore } from '@/presentation/stores/settingsStore';
import { useDebouncedValue } from '@/presentation/hooks/useInfiniteScroll';

export interface MovimientoItem {
  readonly transaccion: Transaccion;
  readonly detailCount: number;
}

interface UseMovimientosArgs {
  readonly tipo?: TipoTransaccion;
  readonly search: string;
  readonly page: number;
}

export function useMovimientos({ tipo, search, page }: UseMovimientosArgs) {
  const pageSize = useSettingsStore((s) => s.pageSize);
  const debounced = useDebouncedValue(search, 200);
  const [items, setItems] = useState<ReadonlyArray<MovimientoItem>>([]);
  const [loading, setLoading] = useState(true);

  const load = useCallback(() => {
    setLoading(true);
    const repo = getRepositories().transacciones;
    const list = repo.listar({
      ...(tipo ? { tipo } : {}),
      limit: pageSize,
      offset: page * pageSize,
    });
    const enriched = list.map((trx) => ({
      transaccion: trx,
      detailCount: repo.findDetalles(trx.id).length,
    }));
    setItems(enriched);
    setLoading(false);
  }, [tipo, page, pageSize]);

  useEffect(() => {
    load();
  }, [load]);

  const filtered = useMemo(() => {
    const q = debounced.trim().toLowerCase();
    if (!q) return items;
    return items.filter((it) =>
      (it.transaccion.detalle ?? '').toLowerCase().includes(q),
    );
  }, [items, debounced]);

  const hasMore = useMemo(() => items.length === pageSize, [items.length, pageSize]);

  return { items: filtered, loading, hasMore, refresh: load };
}
