import { useCallback, useEffect, useState } from 'react';
import dayjs from 'dayjs';

import { getRepositories } from '@/data/repositories/container';
import type { Transaccion, TipoTransaccion } from '@/domain/entities/Transaccion';
import { useSettingsStore } from '@/presentation/stores/settingsStore';

export interface TransaccionResumen {
  readonly transaccion: Transaccion;
  readonly detalles: ReadonlyArray<{ id: number; nombre: string; cantidad: number; precioUnitario: number }>;
}

export function useTransacciones({ tipo, page }: { tipo?: TipoTransaccion; page: number }) {
  const pageSize = useSettingsStore((s) => s.pageSize);
  const [items, setItems] = useState<ReadonlyArray<TransaccionResumen>>([]);
  const [loading, setLoading] = useState(true);

  const load = useCallback(() => {
    setLoading(true);
    const repo = getRepositories();
    const list = repo.transacciones.listar({ tipo, limit: pageSize, offset: page * pageSize });
    const enriched = list.map((trx) => {
      const detalles = repo.transacciones.findDetalles(trx.id);
      const productoIds = detalles.map((d) => d.productoId);
      const productos = repo.productos.findByIds(productoIds);
      const map = new Map(productos.map((p) => [p.id, p.nombre]));
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
    setItems(enriched);
    setLoading(false);
  }, [tipo, page, pageSize]);

  useEffect(() => {
    load();
  }, [load]);

  return { items, loading, refresh: load, hasMore: items.length === pageSize };
}

export function useGastosDelMes(page: number) {
  const pageSize = useSettingsStore((s) => s.pageSize);
  const [items, setItems] = useState<ReadonlyArray<Transaccion>>([]);
  const [loading, setLoading] = useState(true);

  const load = useCallback(() => {
    setLoading(true);
    const list = getRepositories().transacciones.gastosDelMes(pageSize, page * pageSize);
    setItems(list);
    setLoading(false);
  }, [page, pageSize]);

  useEffect(() => {
    load();
  }, [load]);

  return { items, loading, refresh: load, hasMore: items.length === pageSize };
}

export function resumenPeriodo() {
  return {
    label: dayjs().format('MMMM YYYY'),
  };
}
