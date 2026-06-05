import React, { useCallback, useEffect, useState } from 'react';
import { ScrollView, Text, View } from 'react-native';
import { useLocalSearchParams, useRouter } from 'expo-router';

import { Card } from '@/presentation/components/ui/Card';
import { Button } from '@/presentation/components/ui/Button';
import { ConfirmDialog } from '@/presentation/components/feedback/ConfirmDialog';
import { Skeleton } from '@/presentation/components/ui/Skeleton';
import { revertirTransaccion } from '@/application/ventas/RevertirTransaccion';
import { getRepositories } from '@/data/repositories/container';
import { useCurrency } from '@/presentation/hooks/useCurrency';
import { usePinGate } from '@/presentation/hooks/usePinGate';
import { useInvalidationStore } from '@/presentation/stores/invalidationStore';
import { formatFecha } from '@/shared/utils/date';
import { ToastService } from '@/infrastructure/toast/ToastService';
import type { Transaccion, TipoPago } from '@/domain/entities/Transaccion';
import type { DetalleTransaccion } from '@/domain/entities/DetalleTransaccion';

interface DetalleEnriquecido {
  readonly id: number;
  readonly nombre: string;
  readonly cantidad: number;
  readonly precioUnitario: number;
}

const TIPO_PAGO_LABEL: Readonly<Record<TipoPago, string>> = {
  EFECTIVO: 'Efectivo',
  TARJETA: 'Tarjeta',
  TRANSFERENCIA: 'Transferencia',
  QR: 'QR',
};

export default function TransaccionDetailScreen() {
  const { id } = useLocalSearchParams<{ id: string }>();
  const router = useRouter();
  const { format } = useCurrency();
  const [confirmOpen, setConfirmOpen] = useState(false);
  const [reverting, setReverting] = useState(false);
  const pinGate = usePinGate();
  const invalidateMany = useInvalidationStore((s) => s.invalidateMany);

  const trxId = id ? Number(id) : null;
  const [transaccion, setTransaccion] = useState<Transaccion | null>(null);
  const [detalles, setDetalles] = useState<ReadonlyArray<DetalleEnriquecido>>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    if (!trxId || Number.isNaN(trxId)) {
      setLoading(false);
      return;
    }
    const repo = getRepositories();
    const trx = repo.transacciones.findById(trxId);
    setTransaccion(trx);
    if (trx) {
      const dets: ReadonlyArray<DetalleTransaccion> = repo.transacciones.findDetalles(trxId);
      const ids = dets.map((d) => d.productoId);
      const productos = repo.productos.findByIds(ids);
      const nameMap = new Map(productos.map((p) => [p.id, p.nombre] as const));
      setDetalles(
        dets.map((d) => ({
          id: d.id,
          nombre: nameMap.get(d.productoId) ?? '—',
          cantidad: d.cantidad,
          precioUnitario: d.precioUnitario,
        })),
      );
    } else {
      setDetalles([]);
    }
    setLoading(false);
  }, [trxId]);

  const handleRevert = useCallback(() => {
    if (!trxId || reverting) return;
    pinGate.request({
      title: 'Revertir transacción',
      onSuccess: () => {
        setReverting(true);
        try {
          revertirTransaccion(trxId);
          invalidateMany(['transacciones', 'caja', 'productos']);
          ToastService.success('Transacción revertida');
          router.back();
        } catch (e) {
          ToastService.error('Error', e instanceof Error ? e.message : 'No se pudo revertir.');
          setReverting(false);
        }
      },
    });
  }, [pinGate, router, reverting, trxId]);

  if (loading) {
    return (
      <ScrollView
        className="flex-1 bg-surface"
        contentContainerClassName="gap-3 p-4"
        accessibilityLabel="Cargando detalle de transacción"
      >
        <Card>
          <Skeleton className="mb-2 h-3 w-16" />
          <Skeleton className="mb-3 h-10 w-44" />
          <Skeleton className="mb-2 h-3 w-32" />
          <Skeleton className="h-3 w-24" />
        </Card>
        <Card>
          <Skeleton className="mb-3 h-3 w-20" />
          {Array.from({ length: 4 }).map((_, i) => (
            <View key={i} className="flex-row items-center justify-between border-b border-border-subtle py-2 last:border-b-0">
              <View className="flex-1 gap-1.5 pr-2">
                <Skeleton className="h-3.5 w-3/4" />
                <Skeleton className="h-3 w-1/2" />
              </View>
              <Skeleton className="h-3.5 w-16" />
            </View>
          ))}
        </Card>
        <Skeleton className="mt-2 h-12 w-full rounded-xl" />
      </ScrollView>
    );
  }

  if (!transaccion) {
    return (
      <View className="flex-1 items-center justify-center bg-surface p-6">
        <Text className="text-base font-semibold text-ink-strong">No encontrada</Text>
        <Text className="mt-1 text-sm text-ink-muted">La transacción #{trxId ?? '?'} no existe o fue eliminada.</Text>
        <View className="mt-4 w-full">
          <Button title="Volver" variant="secondary" onPress={() => router.back()} fullWidth />
        </View>
      </View>
    );
  }

  const sign = transaccion.tipo === 'VENTA' ? '+' : '-';
  const amountClass =
    transaccion.tipo === 'VENTA'
      ? 'text-success'
      : transaccion.tipo === 'COMPRA'
        ? 'text-warning'
        : 'text-danger';

  return (
    <ScrollView className="flex-1 bg-surface" contentContainerClassName="gap-3 p-4">
      <Card>
        <Text className="text-xs font-semibold uppercase tracking-wide text-ink">{transaccion.tipo}</Text>
        <Text className={`mt-1 text-4xl font-extrabold ${amountClass}`}>
          {sign}{format(Math.abs(transaccion.montoTotal))}
        </Text>
        <Text className="mt-1 text-xs text-ink-muted">{formatFecha(transaccion.fechaRegistro)}</Text>
        <Text className="mt-2 text-sm text-ink">
          Pago: <Text className="font-semibold text-ink-strong">{TIPO_PAGO_LABEL[transaccion.tipoPago]}</Text>
        </Text>
        {transaccion.detalle ? (
          <Text className="mt-1 text-sm text-ink">
            Detalle: <Text className="font-semibold text-ink-strong">{transaccion.detalle}</Text>
          </Text>
        ) : null}
      </Card>

      {detalles.length > 0 ? (
        <Card>
          <Text className="mb-1 text-sm font-semibold uppercase tracking-wide text-ink">
            Productos
          </Text>
          {detalles.map((d) => (
            <View
              key={d.id}
              className="flex-row items-center justify-between border-b border-border-subtle py-2 last:border-b-0"
            >
              <View className="flex-1 pr-2">
                <Text className="text-sm font-semibold text-ink-strong">{d.nombre}</Text>
                <Text className="text-xs text-ink-muted">
                  {d.cantidad} × {format(d.precioUnitario)}
                </Text>
              </View>
              <Text className="font-bold text-ink-strong">
                {format(d.cantidad * d.precioUnitario)}
              </Text>
            </View>
          ))}
        </Card>
      ) : null}

      <Button
        title="Revertir transacción"
        variant="danger"
        onPress={() => setConfirmOpen(true)}
        busy={reverting}
        disabled={reverting}
        fullWidth
      />

      <ConfirmDialog
        visible={confirmOpen}
        title="Revertir"
        message="Esta acción restaurará el stock y ajustará la caja. No se puede deshacer."
        destructive
        onCancel={() => setConfirmOpen(false)}
        onConfirm={() => {
          setConfirmOpen(false);
          handleRevert();
        }}
      />
    </ScrollView>
  );
}
