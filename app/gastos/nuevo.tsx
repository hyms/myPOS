import React, { useCallback, useState } from 'react';
import { Pressable, ScrollView, Text, View } from 'react-native';
import { router } from 'expo-router';

import { Button } from '@/presentation/components/ui/Button';
import { Input } from '@/presentation/components/ui/Input';
import { ConfirmDialog } from '@/presentation/components/feedback/ConfirmDialog';
import { registrarGasto } from '@/application/gastos/RegistrarGasto';
import { useCurrency } from '@/presentation/hooks/useCurrency';
import { ToastService } from '@/infrastructure/toast/ToastService';
import { generarYCompartirComprobante } from '@/infrastructure/pdf/ComprobanteService';
import { useInvalidationStore } from '@/presentation/stores/invalidationStore';
import type { TipoPago } from '@/domain/entities/Transaccion';
import { cn } from '@/shared/utils/cn';
import { DARK_PALETTE } from '@/presentation/theme/tokens';

const TIPOS: ReadonlyArray<{ value: TipoPago; label: string }> = [
  { value: 'EFECTIVO', label: 'Efectivo' },
  { value: 'TARJETA', label: 'Tarjeta' },
  { value: 'TRANSFERENCIA', label: 'Transferencia' },
  { value: 'QR', label: 'QR' },
];

export default function NuevoGastoScreen() {
  const { format, currency } = useCurrency();
  const invalidateMany = useInvalidationStore((s) => s.invalidateMany);
  const [monto, setMonto] = useState('');
  const [descripcion, setDescripcion] = useState('');
  const [tipoPago, setTipoPago] = useState<TipoPago>('EFECTIVO');
  const [confirmOpen, setConfirmOpen] = useState(false);
  const [busy, setBusy] = useState(false);

  const montoNum = Number(monto) || 0;

  const handleConfirm = useCallback(async () => {
    setBusy(true);
    try {
      const result = registrarGasto({
        monto: montoNum,
        descripcion: descripcion.trim(),
        tipoPago,
      });
      await generarYCompartirComprobante({
        tipo: 'GASTO',
        transaccionId: result.transaccionId,
        monto: montoNum,
        descripcion: descripcion.trim(),
        tipoPago,
        currency,
      });
      ToastService.success('Gasto registrado', format(montoNum));
      invalidateMany(['transacciones', 'caja']);
      setConfirmOpen(false);
      router.back();
    } catch (e) {
      ToastService.error('Error', e instanceof Error ? e.message : 'No se pudo registrar.');
    } finally {
      setBusy(false);
    }
  }, [currency, descripcion, format, montoNum, tipoPago]);

  return (
    <ScrollView
      className="flex-1 bg-canvas"
      contentContainerClassName="gap-4 p-5"
      keyboardShouldPersistTaps="handled"
    >
      <View>
        <Text className="mb-1 text-sm font-medium text-ink">Monto</Text>
        <View className="rounded-2xl border-2 border-danger bg-surface p-4">
          <Text className="text-3xl font-bold text-danger">{format(montoNum)}</Text>
        </View>
      </View>
      <Input
        label="Monto (entrada)"
        value={monto}
        onChangeText={(t) => setMonto(t.replace(/[^0-9.]/g, ''))}
        keyboardType="decimal-pad"
        placeholder="0.00"
        autoFocus
      />
      <Input
        label="Descripción"
        value={descripcion}
        onChangeText={setDescripcion}
        placeholder="Ej. Pago de luz"
        multiline
        numberOfLines={3}
        style={{ minHeight: 80, textAlignVertical: 'top' }}
      />
      <View>
        <Text className="mb-1 text-sm font-medium text-ink">Tipo de pago</Text>
        <View className="flex-row flex-wrap gap-2">
          {TIPOS.map((t) => {
            const active = t.value === tipoPago;
            return (
              <Pressable
                key={t.value}
                onPress={() => setTipoPago(t.value)}
                className={cn(
                  'rounded-lg border px-3 py-2',
                  active
                    ? 'border-accent bg-accent'
                    : 'border-border bg-surface',
                )}
              >
                <Text
                  className={cn(
                    'text-sm font-semibold',
                    active ? 'text-onAccent' : 'text-ink-strong',
                  )}
                >
                  {t.label}
                </Text>
              </Pressable>
            );
          })}
        </View>
      </View>
      <Button
        title="Añadir"
        variant="danger"
        onPress={() => setConfirmOpen(true)}
        disabled={montoNum <= 0 || descripcion.trim().length === 0}
        fullWidth
      />
      <ConfirmDialog
        visible={confirmOpen}
        title="Confirmar gasto"
        message={`¿Está seguro de registrar este gasto por un monto de ${format(montoNum)} con el detalle "${descripcion.trim()}"?`}
        destructive
        confirmLabel="Confirmar"
        cancelLabel="Cancelar"
        onConfirm={handleConfirm}
        onCancel={() => setConfirmOpen(false)}
        busy={busy}
      />
    </ScrollView>
  );
}
