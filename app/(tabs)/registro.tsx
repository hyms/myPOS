import React, { useCallback } from 'react';
import { View } from 'react-native';

import { CarritoScreen } from '@/presentation/screens/CarritoScreen';
import { RegistroSegmented } from '@/presentation/components/registro/RegistroSegmented';
import { useCompraCarritoStore, useVentaCarritoStore, type CarritoStoreHook } from '@/presentation/stores/carritoStore';
import { useRegistroSubtabStore } from '@/presentation/stores/registroSubtabStore';
import type { CarritoTipo } from '@/domain/entities/CarritoItem';

export default function RegistroTab() {
  const tipo = useRegistroSubtabStore((s) => s.tipo);
  const setTipo = useRegistroSubtabStore((s) => s.setTipo);

  const handleChange = useCallback(
    (next: CarritoTipo) => {
      setTipo(next);
    },
    [setTipo],
  );

  useVentaCarritoStore((s) => s.items);
  useCompraCarritoStore((s) => s.items);
  const store: CarritoStoreHook =
    tipo === 'VENTA' ? useVentaCarritoStore : useCompraCarritoStore;

  return (
    <View className="flex-1 bg-surface">
      <RegistroSegmented value={tipo} onChange={handleChange} />
      <View className="flex-1 -mt-3">
        <CarritoScreen tipo={tipo} store={store} />
      </View>
    </View>
  );
}
