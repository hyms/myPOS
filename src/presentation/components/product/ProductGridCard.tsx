import React, { memo, useCallback } from 'react';
import { Image, Pressable, Text, View } from 'react-native';

import type { Producto } from '@/domain/entities/Producto';
import { isBelowMin } from '@/domain/rules/stockRules';
import { useCurrency } from '@/presentation/hooks/useCurrency';
import { ProductFallbackAvatar } from './ProductFallbackAvatar';

interface Props {
  readonly producto: Pick<Producto, 'id' | 'nombre' | 'precioVenta' | 'stockActual' | 'stockMinimo' | 'imagenUri'>;
  readonly onPress: (id: number) => void;
}

function ProductGridCardComponent({ producto, onPress }: Props) {
  const { format } = useCurrency();
  const lowStock = isBelowMin(producto);

  const handlePress = useCallback(() => {
    onPress(producto.id);
  }, [onPress, producto.id]);

  return (
    <Pressable
      onPress={handlePress}
      className="m-1 flex-1 overflow-hidden rounded-2xl border border-surface-100 bg-white active:bg-surface-50 dark:border-surface-800 dark:bg-surface-900 dark:active:bg-surface-800"
      accessibilityRole="button"
      accessibilityLabel={`${producto.nombre}, ${format(producto.precioVenta)}`}
    >
      <View className="aspect-square w-full items-center justify-center bg-surface-50 dark:bg-surface-800">
        {producto.imagenUri ? (
          <Image
            source={{ uri: producto.imagenUri }}
            className="h-full w-full"
            resizeMode="cover"
          />
        ) : (
          <ProductFallbackAvatar id={producto.id} nombre={producto.nombre} size={64} />
        )}
        {lowStock ? (
          <View className="absolute left-1 top-1 rounded-full bg-danger-500 px-2 py-0.5">
            <Text className="text-[10px] font-bold text-white">BAJO</Text>
          </View>
        ) : null}
      </View>
      <View className="px-2 py-2">
        <Text numberOfLines={1} className="text-sm font-semibold text-surface-900 dark:text-surface-50">
          {producto.nombre}
        </Text>
        <Text className="mt-0.5 text-sm font-bold text-primary-700 dark:text-primary-300">
          {format(producto.precioVenta)}
        </Text>
        <Text className="mt-0.5 text-[11px] text-surface-500">Stock: {producto.stockActual}</Text>
      </View>
    </Pressable>
  );
}

export const ProductGridCard = memo(ProductGridCardComponent);
