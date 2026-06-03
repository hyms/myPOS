import React, { memo, useCallback, useEffect, useState } from 'react';
import { Pressable, Text, View } from 'react-native';

import type { CarritoItem } from '@/domain/entities/CarritoItem';
import { useCarritoStore } from '@/presentation/stores/carritoStore';
import { useCurrency } from '@/presentation/hooks/useCurrency';
import { Button } from '@/presentation/components/ui/Button';
import { Input } from '@/presentation/components/ui/Input';
import { ModalSheet } from '@/presentation/components/ui/ModalSheet';
import { getUnitPrice } from '@/domain/rules/cartRules';
import { cn } from '@/shared/utils/cn';

interface Props {
  readonly visible: boolean;
  readonly item: CarritoItem | null;
  readonly onClose: () => void;
}

function LineItemModalComponent({ visible, item, onClose }: Props) {
  const actualizar = useCarritoStore((s) => s.actualizar);
  const tipo = useCarritoStore((s) => s.tipo);
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

  return (
    <ModalSheet visible={visible} onClose={onClose} title={item.producto.nombre}>
      <View className="gap-3">
        <View className="rounded-lg bg-surface-50 p-3 dark:bg-surface-800">
          <Text className="text-sm text-surface-500">Precio unitario</Text>
          <Text className="text-lg font-bold text-surface-900 dark:text-surface-50">{format(unit)}</Text>
        </View>
        <Input
          label="Cantidad"
          value={cantidad}
          onChangeText={(t) => setCantidad(t.replace(/[^0-9]/g, ''))}
          keyboardType="number-pad"
        />
        <Input
          label="Descuento (monto fijo)"
          value={descuento}
          onChangeText={(t) => setDescuento(t.replace(/[^0-9.]/g, ''))}
          keyboardType="decimal-pad"
          helper="No puede superar el subtotal"
        />
        <View className="rounded-lg bg-primary-50 p-3">
          <Text className="text-sm text-primary-700">Subtotal línea</Text>
          <Text className="text-2xl font-bold text-primary-700">{format(Math.max(0, subtotal))}</Text>
        </View>
        <View className={cn('flex-row gap-2')}>
          <Button title="Cancelar" variant="secondary" onPress={onClose} className="flex-1" />
          <Button title="Aplicar" onPress={handleAccept} className="flex-1" />
        </View>
      </View>
    </ModalSheet>
  );
}

export const LineItemModal = memo(LineItemModalComponent);
