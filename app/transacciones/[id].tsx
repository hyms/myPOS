import React, { useCallback, useEffect, useState } from 'react';
import { ActivityIndicator, ScrollView, Text, View } from 'react-native';
import { useLocalSearchParams, useRouter } from 'expo-router';

import { Card } from '@/presentation/components/ui/Card';
import { Button } from '@/presentation/components/ui/Button';
import { ConfirmDialog } from '@/presentation/components/feedback/ConfirmDialog';
import { revertirTransaccion } from '@/application/ventas/RevertirTransaccion';
import { getRepositories } from '@/data/repositories/container';
import { useCurrency } from '@/presentation/hooks/useCurrency';
import { usePinGate } from '@/presentation/hooks/usePinGate';
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
  const pinGate = usePinGate();

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
    if (!trxId) return;
    pinGate.request({
      title: 'Revertir transacción',
      onSuccess: () => {
        try {
          revertirTransaccion(trxId);
          ToastService.success('Transacción revertida');
          router.back();
        } catch (e) {
          ToastService.error('Error', e instanceof Error ? e.message : 'No se pudo revertir.');
        }
      },
    });
  }, [pinGate, router, trxId]);

  if (loading) {
    return (
      <View className="flex-1 items-center justify-center bg-surface-50 dark:bg-surface-950">
        <ActivityIndicator color="#0ea5e9" />
      </View>
    );
  }

  if (!transaccion) {
    return (
      <View className="flex-1 items-center justify-center bg-surface-50 p-6">
        <Text className="text-base font-semibold text-surface-800 dark:text-surface-100">No encontrada</Text>
        <Text className="mt-1 text-sm text-surface-600">La transacción #{trxId ?? '?'} no existe o fue eliminada.</Text>
        <View className="mt-4 w-full">
          <Button title="Volver" variant="secondary" onPress={() => router.back()} fullWidth />
        </View>
      </View>
    );
  }

  const sign = transaccion.tipo === 'VENTA' ? '+' : '-';
  const amountClass =
    transaccion.tipo === 'VENTA'
      ? 'text-success-800'
      : transaccion.tipo === 'COMPRA'
        ? 'text-warning-800'
        : 'text-danger-800';

  return (
    <ScrollView className="flex-1 bg-surface-50 dark:bg-surface-950" contentContainerClassName="gap-3 p-4">
      <Card>
        <Text className="text-xs font-semibold uppercase tracking-wide text-surface-700">{transaccion.tipo}</Text>
        <Text className={`mt-1 text-4xl font-extrabold ${amountClass}`}>
          {sign}{format(Math.abs(transaccion.montoTotal))}
        </Text>
        <Text className="mt-1 text-xs text-surface-600">{formatFecha(transaccion.fechaRegistro)}</Text>
        <Text className="mt-2 text-sm text-surface-700 dark:text-surface-200">
          Pago: <Text className="font-semibold text-surface-900 dark:text-surface-50">{TIPO_PAGO_LABEL[transaccion.tipoPago]}</Text>
        </Text>
        {transaccion.detalle ? (
          <Text className="mt-1 text-sm text-surface-700 dark:text-surface-200">
            Detalle: <Text className="font-semibold text-surface-900 dark:text-surface-50">{transaccion.detalle}</Text>
          </Text>
        ) : null}
      </Card>

      {detalles.length > 0 ? (
        <Card>
          <Text className="mb-1 text-sm font-semibold uppercase tracking-wide text-surface-700">
            Productos
          </Text>
          {detalles.map((d) => (
            <View
              key={d.id}
              className="flex-row items-center justify-between border-b border-surface-100 py-2 last:border-b-0 dark:border-surface-800"
            >
              <View className="flex-1 pr-2">
                <Text className="text-sm font-semibold text-surface-800 dark:text-surface-100">{d.nombre}</Text>
                <Text className="text-xs text-surface-500">
                  {d.cantidad} × {format(d.precioUnitario)}
                </Text>
              </View>
              <Text className="font-bold text-surface-900 dark:text-surface-50">
                {format(d.cantidad * d.precioUnitario)}
              </Text>
            </View>
          ))}
        </Card>
      ) : null}

      <Button title="Revertir transacción" variant="danger" onPress={() => setConfirmOpen(true)} fullWidth />

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
