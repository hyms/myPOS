import React, { memo, useCallback, useEffect, useState } from 'react';
import { Text, View } from 'react-native';

import type { CarritoItem, CarritoTipo } from '@/domain/entities/CarritoItem';
import type { CarritoStoreHook } from '@/presentation/stores/carritoStore';
import { useCurrency } from '@/presentation/hooks/useCurrency';
import { Button } from '@/presentation/components/ui/Button';
import { Input } from '@/presentation/components/ui/Input';
import { ModalSheet } from '@/presentation/components/ui/ModalSheet';
import { getUnitPrice } from '@/domain/rules/cartRules';
import { cn } from '@/shared/utils/cn';

interface Props {
  readonly visible: boolean;
  readonly item: CarritoItem | null;
  readonly store: CarritoStoreHook;
  readonly tipo: CarritoTipo;
  readonly onClose: () => void;
}

function LineItemModalComponent({ visible, item, store, tipo, onClose }: Props) {
  const actualizar = store((s) => s.actualizar);
  const { format } = useCurrency();
  const [cantidad, setCantidad] = useState('1');
  const [descuento, setDescuento] = useState('0');

  useEffect(() => {
    if (item) {
      setCantidad(String(item.cantidad));
      setDescuento(String(item.descuento));
    }
  }, [item]);

  const handleAccept = useCallback(() => {
    if (!item) return;
    const cant = Math.max(1, Math.floor(Number(cantidad) || 1));
    const desc = Math.max(0, Number(descuento) || 0);
    actualizar(item.producto.id, cant, desc);
    onClose();
  }, [actualizar, cantidad, descuento, item, onClose]);

  if (!item) return null;
  const unit = getUnitPrice(item, tipo);
  const cantNum = Math.max(0, Math.floor(Number(cantidad) || 0));
  const descNum = Math.max(0, Number(descuento) || 0);
  const subtotal = unit * cantNum - descNum;
  const maxCant = tipo === 'VENTA' ? item.producto.stockActual : Infinity;
  const cantExcedeStock = tipo === 'VENTA' && cantNum > item.producto.stockActual;
  const descExcedeSubtotal = descNum > unit * cantNum;
  const invalid = cantExcedeStock || descExcedeSubtotal || cantNum < 1;

  return (
    <ModalSheet visible={visible} onClose={onClose} title={item.producto.nombre}>
      <View className="gap-3">
        <View className="rounded-lg bg-surface-50 p-3 dark:bg-surface-800">
          <Text className="text-xs font-medium uppercase tracking-wide text-surface-500">
            Precio unitario
          </Text>
          <Text className="text-lg font-bold tabular-nums text-surface-900 dark:text-surface-50">
            {format(unit)}
          </Text>
        </View>
        <Input
          label="Cantidad"
          value={cantidad}
          onChangeText={(t) => setCantidad(t.replace(/[^0-9]/g, ''))}
          keyboardType="number-pad"
          {...(tipo === 'VENTA' ? { helper: `Stock disponible: ${item.producto.stockActual}` } : {})}
          {...(cantExcedeStock ? { error: `Máximo disponible: ${item.producto.stockActual}` } : {})}
        />
        <Input
          label="Descuento (monto fijo)"
          value={descuento}
          onChangeText={(t) => setDescuento(t.replace(/[^0-9.]/g, ''))}
          keyboardType="decimal-pad"
          {...(descExcedeSubtotal
            ? { error: `El descuento no puede superar ${format(unit * cantNum)}` }
            : {})}
        />
        <View
          accessibilityLabel={`Subtotal de la línea: ${format(Math.max(0, subtotal))}`}
          className="rounded-xl border border-primary-200 bg-primary-50 p-3 dark:border-primary-800 dark:bg-primary-950"
        >
          <Text className="text-xs font-semibold uppercase tracking-wide text-primary-700">
            Subtotal línea
          </Text>
          <Text className="text-2xl font-bold tabular-nums text-primary-700 dark:text-primary-300">
            {format(Math.max(0, subtotal))}
          </Text>
        </View>
        <View className={cn('flex-row gap-2')}>
          <Button title="Cancelar" variant="secondary" onPress={onClose} className="flex-1" />
          <Button title="Aplicar" onPress={handleAccept} className="flex-1" disabled={invalid} />
        </View>
      </View>
    </ModalSheet>
  );
}

export const LineItemModal = memo(LineItemModalComponent);
