import React, { useCallback, useState } from 'react';
import { RefreshControl, Text, View } from 'react-native';
import { FlashList } from '@shopify/flash-list';
import type { ListRenderItem } from '@shopify/flash-list';
import { router } from 'expo-router';

import { useMovimientos, type MovimientoItem } from '@/presentation/hooks/useMovimientos';
import { useCurrency } from '@/presentation/hooks/useCurrency';
import { useInfiniteScroll } from '@/presentation/hooks/useInfiniteScroll';
import { useSettingsStore } from '@/presentation/stores/settingsStore';
import { SearchBar } from '@/presentation/components/filters/SearchBar';
import { TipoFilterChips, type TipoFilter } from '@/presentation/components/movimientos/TipoFilterChips';
import { MovimientoListItem } from '@/presentation/components/movimientos/MovimientoListItem';
import { EmptyState } from '@/presentation/components/feedback/EmptyState';
import { Skeleton } from '@/presentation/components/ui/Skeleton';
import type { TipoTransaccion } from '@/domain/entities/Transaccion';

const TIPO_MAP: Readonly<Record<Exclude<TipoFilter, 'ALL'>, TipoTransaccion>> = {
  VENTA: 'VENTA',
  COMPRA: 'COMPRA',
  GASTO: 'GASTO',
};

export default function MovimientosIndexScreen() {
  const { format } = useCurrency();
  const pageSize = useSettingsStore((s) => s.pageSize);
  const [page, setPage] = useState(0);
  const [tipoFilter, setTipoFilter] = useState<TipoFilter>('ALL');
  const [search, setSearch] = useState('');

  const tipoArg = tipoFilter === 'ALL' ? undefined : TIPO_MAP[tipoFilter];
  const { items, loading, hasMore, refresh } = useMovimientos({
    ...(tipoArg ? { tipo: tipoArg } : {}),
    search,
    page,
  });
  const { onEndReached } = useInfiniteScroll(() => setPage((p) => p + 1), hasMore, loading);

  const handleTipoChange = useCallback((next: TipoFilter) => {
    setTipoFilter(next);
    setPage(0);
  }, []);

  const handleSearchChange = useCallback((v: string) => {
    setSearch(v);
    setPage(0);
  }, []);

  const handleOpen = useCallback((id: number) => {
    router.push(`/transacciones/${id}` as never);
  }, []);

  const renderItem: ListRenderItem<MovimientoItem> = useCallback(
    ({ item }) => (
      <MovimientoListItem item={item.transaccion} detailCount={item.detailCount} onPress={handleOpen} format={format} />
    ),
    [handleOpen, format],
  );

  const keyExtractor = useCallback((item: MovimientoItem) => String(item.transaccion.id), []);

  return (
    <View className="flex-1 bg-surface-50 dark:bg-surface-950">
      <View className="bg-surface-50 px-3 pt-2 dark:bg-surface-950">
        <SearchBar
          value={search}
          onChangeText={handleSearchChange}
          placeholder="Buscar por detalle…"
        />
        <TipoFilterChips value={tipoFilter} onChange={handleTipoChange} />
        <Text className="px-1 pb-2 text-[11px] text-surface-500">
          {`Mostrando ${items.length}${hasMore ? '+' : ''} · página ${page + 1} · lote ${pageSize}`}
        </Text>
      </View>

      {loading && items.length === 0 ? (
        <View className="gap-2 p-3">
          {Array.from({ length: 6 }).map((_, i) => (
            <Skeleton key={i} className="h-20 rounded-2xl" />
          ))}
        </View>
      ) : (
        <FlashList
          data={items}
          keyExtractor={keyExtractor}
          renderItem={renderItem}
          onEndReached={onEndReached}
          onEndReachedThreshold={0.5}
          refreshControl={<RefreshControl refreshing={loading} onRefresh={refresh} />}
          contentContainerClassName="pb-6 pt-1"
          ListEmptyComponent={
            <EmptyState
              icon="document-text-outline"
              title="Sin movimientos"
              description={search ? 'No hay coincidencias para tu búsqueda.' : 'Aún no registras ventas, compras ni gastos.'}
            />
          }
        />
      )}
    </View>
  );
}
