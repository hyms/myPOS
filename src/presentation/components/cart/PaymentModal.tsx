import React, { memo, useCallback, useEffect, useMemo, useState } from 'react';
import { Pressable, Text, View } from 'react-native';

import type { CarritoItem, CarritoTipo } from '@/domain/entities/CarritoItem';
import type { TipoPago } from '@/domain/entities/Transaccion';
import { calcChange, calcTotals } from '@/domain/rules/cartRules';
import { useCurrency } from '@/presentation/hooks/useCurrency';
import { Button } from '@/presentation/components/ui/Button';
import { Input } from '@/presentation/components/ui/Input';
import { ModalSheet } from '@/presentation/components/ui/ModalSheet';
import { cn } from '@/shared/utils/cn';

interface Props {
  readonly visible: boolean;
  readonly items: ReadonlyArray<CarritoItem>;
  readonly tipo: CarritoTipo;
  readonly onClose: () => void;
  readonly onConfirm: (params: { tipoPago: TipoPago; recibido: number | null }) => void | Promise<void>;
}

const TIPOS_PAGO: ReadonlyArray<{ value: TipoPago; label: string }> = [
  { value: 'EFECTIVO', label: 'Efectivo' },
  { value: 'TARJETA', label: 'Tarjeta' },
  { value: 'TRANSFERENCIA', label: 'Transferencia' },
  { value: 'QR', label: 'QR' },
];

function PaymentModalComponent({ visible, items, tipo, onClose, onConfirm }: Props) {
  const { format } = useCurrency();
  const totals = useMemo(() => calcTotals(items, tipo), [items, tipo]);
  const [tipoPago, setTipoPago] = useState<TipoPago>('EFECTIVO');
  const [recibido, setRecibido] = useState('');
  const [busy, setBusy] = useState(false);

  useEffect(() => {
    if (visible) {
      setTipoPago('EFECTIVO');
      setRecibido('');
      setBusy(false);
    }
  }, [visible]);

  const recibidoNum = useMemo(() => Math.max(0, Number(recibido) || 0), [recibido]);
  const cambio = useMemo(
    () => (tipoPago === 'EFECTIVO' ? calcChange(totals.total, recibidoNum) : 0),
    [tipoPago, totals.total, recibidoNum],
  );
  const suficiente = tipoPago !== 'EFECTIVO' || recibidoNum >= totals.total;

  const handleConfirm = useCallback(async () => {
    if (!suficiente) return;
    setBusy(true);
    try {
      await onConfirm({
        tipoPago,
        recibido: tipoPago === 'EFECTIVO' ? recibidoNum : null,
      });
    } finally {
      setBusy(false);
    }
  }, [onConfirm, recibidoNum, suficiente, tipoPago]);

  return (
    <ModalSheet
      visible={visible}
      onClose={onClose}
      title={tipo === 'VENTA' ? 'Cobrar venta' : 'Pagar compra'}
    >
      <View className="gap-3">
        <View className="rounded-lg bg-surface-50 p-3 dark:bg-surface-800">
          <Text className="text-sm text-surface-500">Total a {tipo === 'VENTA' ? 'cobrar' : 'pagar'}</Text>
          <Text className="text-3xl font-bold text-primary-700">{format(totals.total)}</Text>
        </View>

        <View>
          <Text className="mb-1 text-sm font-medium text-surface-700 dark:text-surface-200">
            Tipo de pago
          </Text>
          <View className="flex-row flex-wrap gap-2">
            {TIPOS_PAGO.map((tp) => {
              const active = tp.value === tipoPago;
              return (
                <Pressable
                  key={tp.value}
                  onPress={() => setTipoPago(tp.value)}
                  className={cn(
                    'rounded-lg border px-3 py-2',
                    active
                      ? 'border-primary-600 bg-primary-600'
                      : 'border-surface-300 bg-white dark:border-surface-700 dark:bg-surface-900',
                  )}
                >
                  <Text
                    className={cn(
                      'text-sm font-semibold',
                      active ? 'text-white' : 'text-surface-800 dark:text-surface-100',
                    )}
                  >
                    {tp.label}
                  </Text>
                </Pressable>
              );
            })}
          </View>
        </View>

        {tipoPago === 'EFECTIVO' ? (
          <>
            <Input
              label="Monto recibido"
              value={recibido}
              onChangeText={(t) => setRecibido(t.replace(/[^0-9.]/g, ''))}
              keyboardType="decimal-pad"
              rightAdornment={
                <Pressable onPress={() => setRecibido(String(totals.total))} hitSlop={8}>
                  <Text className="text-sm font-semibold text-primary-600">Exacto</Text>
                </Pressable>
              }
            />
            <View
              className={cn(
                'rounded-lg p-3',
                cambio >= 0 ? 'bg-success-50' : 'bg-danger-50',
              )}
            >
              <Text className="text-sm text-surface-500">Cambio</Text>
              <Text
                className={cn(
                  'text-2xl font-bold',
                  cambio >= 0 ? 'text-success-700' : 'text-danger-700',
                )}
              >
                {format(Math.abs(cambio))}
              </Text>
            </View>
          </>
        ) : null}

        <View className="flex-row gap-2">
          <Button title="Cancelar" variant="secondary" onPress={onClose} className="flex-1" />
          <Button
            title={tipo === 'VENTA' ? 'Cobrar' : 'Registrar'}
            onPress={handleConfirm}
            disabled={!suficiente || busy}
            busy={busy}
            className="flex-1"
          />
        </View>
      </View>
    </ModalSheet>
  );
}

export const PaymentModal = memo(PaymentModalComponent);
