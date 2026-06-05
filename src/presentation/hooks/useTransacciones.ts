import { useCallback, useEffect, useState } from 'react';

import { getRepositories } from '@/data/repositories/container';
import type { Transaccion, TipoTransaccion } from '@/domain/entities/Transaccion';
import { useSettingsStore } from '@/presentation/stores/settingsStore';
import { useInvalidationStore } from '@/presentation/stores/invalidationStore';

export interface TransaccionResumen {
  readonly transaccion: Transaccion;
  readonly detalles: ReadonlyArray<{ id: number; nombre: string; cantidad: number; precioUnitario: number }>;
}

function loadResumen(
  tipo: TipoTransaccion | undefined,
  page: number,
  pageSize: number,
): ReadonlyArray<TransaccionResumen> {
  const repo = getRepositories();
  const list = repo.transacciones.listar({
    ...(tipo ? { tipo } : {}),
    limit: pageSize,
    offset: page * pageSize,
  });
  return list.map((trx) => {
    const detalles = repo.transacciones.findDetalles(trx.id);
    const productoIds = detalles.map((d) => d.productoId);
    const productos = repo.productos.findByIds(productoIds);
    const map = new Map(productos.map((p) => [p.id, p.nombre] as const));
    return {
      transaccion: trx,
      detalles: detalles.map((d) => ({
        id: d.id,
        nombre: map.get(d.productoId) ?? '—',
        cantidad: d.cantidad,
        precioUnitario: d.precioUnitario,
      })),
    };
  });
}

export function useTransacciones({ tipo, page }: { tipo?: TipoTransaccion; page: number }) {
  const pageSize = useSettingsStore((s) => s.pageSize);
  const invalidVersion = useInvalidationStore((s) => s.versions.transacciones);
  const [items, setItems] = useState<ReadonlyArray<TransaccionResumen>>(() =>
    loadResumen(tipo, page, pageSize),
  );
  const [hasLoaded, setHasLoaded] = useState(false);

  useEffect(() => {
    setItems(loadResumen(tipo, page, pageSize));
    if (!hasLoaded) setHasLoaded(true);
  }, [tipo, page, pageSize, hasLoaded, invalidVersion]);

  const refresh = useCallback(() => {
    setItems(loadResumen(tipo, page, pageSize));
  }, [tipo, page, pageSize]);

  return {
    items,
    loading: !hasLoaded,
    refresh,
    hasMore: items.length === pageSize,
  };
}

function loadGastosDelMes(page: number, pageSize: number): ReadonlyArray<Transaccion> {
  return getRepositories().transacciones.gastosDelMes(pageSize, page * pageSize);
}

export function useGastosDelMes(page: number) {
  const pageSize = useSettingsStore((s) => s.pageSize);
  const invalidVersion = useInvalidationStore((s) => s.versions.transacciones);
  const [items, setItems] = useState<ReadonlyArray<Transaccion>>(() =>
    loadGastosDelMes(page, pageSize),
  );
  const [hasLoaded, setHasLoaded] = useState(false);

  useEffect(() => {
    setItems(loadGastosDelMes(page, pageSize));
    if (!hasLoaded) setHasLoaded(true);
  }, [page, pageSize, hasLoaded, invalidVersion]);

  const refresh = useCallback(() => {
    setItems(loadGastosDelMes(page, pageSize));
  }, [page, pageSize]);

  return {
    items,
    loading: !hasLoaded,
    refresh,
    hasMore: items.length === pageSize,
  };
}
