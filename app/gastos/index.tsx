import React, { useCallback, useEffect, useState } from 'react';
import { FlashList } from '@shopify/flash-list';
import { router } from 'expo-router';
import { Pressable, Text, View } from 'react-native';

import { useGastosDelMes } from '@/presentation/hooks/useTransacciones';
import { useCurrency } from '@/presentation/hooks/useCurrency';
import { useInfiniteScroll } from '@/presentation/hooks/useInfiniteScroll';
import { EmptyState } from '@/presentation/components/feedback/EmptyState';
import { formatFecha } from '@/shared/utils/date';

export default function GastosIndexScreen() {
  const [page, setPage] = useState(0);
  const { items, hasMore, loading, refresh } = useGastosDelMes(page);
  const { format } = useCurrency();

  useEffect(() => {
    refresh();
  }, [refresh]);

  const onEndReached = useInfiniteScroll(() => setPage((p) => p + 1), hasMore, loading);

  const handleNew = useCallback(() => {
    router.push('/gastos/nuevo');
  }, []);

  return (
    <View className="flex-1 bg-surface-50 dark:bg-surface-950">
      <FlashList
        data={items}
        keyExtractor={(item) => String(item.id)}
        renderItem={({ item }) => (
          <Pressable
            onPress={() => router.push(`/transacciones/${item.id}` as never)}
            className="m-2 rounded-2xl border border-surface-100 bg-white p-4 active:bg-surface-50 dark:border-surface-800 dark:bg-surface-900"
          >
            <View className="flex-row items-center justify-between">
              <View className="flex-1 pr-2">
                <Text className="text-base font-semibold text-surface-900 dark:text-surface-50" numberOfLines={2}>
                  {item.detalle ?? '—'}
                </Text>
                <Text className="mt-0.5 text-xs text-surface-500">{formatFecha(item.fechaRegistro)}</Text>
              </View>
              <Text className="text-lg font-bold text-danger-700">-{format(item.montoTotal)}</Text>
            </View>
          </Pressable>
        )}
        onEndReached={onEndReached}
        onEndReachedThreshold={0.5}
        contentContainerClassName="pb-24 pt-2"
        ListEmptyComponent={!loading ? <EmptyState title="Sin gastos este mes" description="Toca + para registrar uno." /> : null}
      />
      <Pressable
        onPress={handleNew}
        accessibilityLabel="Nuevo gasto"
        className="absolute bottom-6 right-6 h-14 w-14 items-center justify-center rounded-full bg-danger-600 shadow-lg active:bg-danger-700"
        style={{ elevation: 6 }}
      >
        <Text className="text-3xl font-bold text-white">+</Text>
      </Pressable>
    </View>
  );
}
