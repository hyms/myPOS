import React, { useCallback, useEffect, useState } from 'react';
import { Pressable, ScrollView, Text, View } from 'react-native';
import { useLocalSearchParams, useRouter } from 'expo-router';

import { Card } from '@/presentation/components/ui/Card';
import { Button } from '@/presentation/components/ui/Button';
import { ConfirmDialog } from '@/presentation/components/feedback/ConfirmDialog';
import { revertirTransaccion } from '@/application/ventas/RevertirTransaccion';
import { useTransacciones } from '@/presentation/hooks/useTransacciones';
import { useCurrency } from '@/presentation/hooks/useCurrency';
import { usePinGate } from '@/presentation/hooks/usePinGate';
import { formatFecha } from '@/shared/utils/date';
import { ToastService } from '@/infrastructure/toast/ToastService';

export default function TransaccionDetailScreen() {
  const { id } = useLocalSearchParams<{ id: string }>();
  const router = useRouter();
  const { format } = useCurrency();
  const { items } = useTransacciones({ page: 0 });
  const [confirmOpen, setConfirmOpen] = useState(false);
  const pinGate = usePinGate();

  const trxId = id ? Number(id) : null;
  const found = items.find((it) => it.transaccion.id === trxId);
  const transaccion = found?.transaccion;

  useEffect(() => {
    if (trxId && !found) {
      // puede que esté en otra página; por simplicidad lo dejamos hasta refresh
    }
  }, [trxId, found]);

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

  if (!transaccion) {
    return (
      <View className="flex-1 items-center justify-center bg-surface-50 p-6">
        <Text className="text-base text-surface-700">Cargando o no encontrada…</Text>
      </View>
    );
  }

  return (
    <ScrollView className="flex-1 bg-surface-50 dark:bg-surface-950" contentContainerClassName="gap-3 p-4">
      <Card>
        <Text className="text-xs font-semibold uppercase text-surface-500">{transaccion.tipo}</Text>
        <Text className="mt-1 text-3xl font-bold text-primary-700">{format(transaccion.montoTotal)}</Text>
        <Text className="mt-1 text-xs text-surface-500">{formatFecha(transaccion.fechaRegistro)}</Text>
        <Text className="mt-1 text-sm text-surface-700 dark:text-surface-200">
          Pago: <Text className="font-semibold">{transaccion.tipoPago}</Text>
        </Text>
        {transaccion.detalle ? (
          <Text className="mt-1 text-sm text-surface-700 dark:text-surface-200">Detalle: {transaccion.detalle}</Text>
        ) : null}
      </Card>

      {found && found.detalles.length > 0 ? (
        <Card>
          <Text className="mb-2 text-sm font-semibold text-surface-700 dark:text-surface-200">Detalle</Text>
          {found.detalles.map((d) => (
            <View
              key={d.id}
              className="flex-row items-center justify-between border-b border-surface-100 py-2 dark:border-surface-800"
            >
              <View className="flex-1 pr-2">
                <Text className="text-sm text-surface-800 dark:text-surface-100">{d.nombre}</Text>
                <Text className="text-xs text-surface-500">{d.cantidad} × {format(d.precioUnitario)}</Text>
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
