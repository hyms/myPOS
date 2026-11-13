import React, { useCallback, useMemo, useState } from 'react';
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
import { AnimatedPressable } from '@/presentation/components/ui/AnimatedPressable';
import { ListFooterLoader, Skeleton } from '@/presentation/components/ui/Skeleton';
import { DARK_PALETTE } from '@/presentation/theme/tokens';
import { cn } from '@/shared/utils/cn';
import type { TipoTransaccion } from '@/domain/entities/Transaccion';

const TIPO_MAP: Readonly<Record<Exclude<TipoFilter, 'ALL'>, TipoTransaccion>> = {
  VENTA: 'VENTA',
  COMPRA: 'COMPRA',
  GASTO: 'GASTO',
};

const FILTERS_INFO: Readonly<Record<TipoFilter, string>> = {
  ALL: 'todos los tipos',
  VENTA: 'ventas',
  COMPRA: 'compras',
  GASTO: 'gastos',
};

const FILTER_TONE: Readonly<Record<TipoFilter, 'success' | 'warning' | 'danger' | 'neutral'>> = {
  ALL: 'neutral',
  VENTA: 'success',
  COMPRA: 'warning',
  GASTO: 'danger',
};

function MovimientosSkeleton() {
  return (
    <View className="gap-2 p-3">
      {Array.from({ length: 5 }).map((_, i) => (
        <View
          key={i}
          className="overflow-hidden rounded-2xl border border-border bg-surface"
        >
          <Skeleton className="h-1 w-full rounded-none" />
          <View className="flex-row items-center justify-between p-3.5">
            <View className="flex-1 gap-2 pr-2">
              <Skeleton className="h-3 w-16" />
              <Skeleton className="h-4 w-3/4" />
              <Skeleton className="h-3 w-24" />
            </View>
            <View className="items-end gap-1.5">
              <Skeleton className="h-5 w-20" />
            </View>
          </View>
        </View>
      ))}
    </View>
  );
}

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

  const totalFiltrado = useMemo(
    () => items.reduce((acc, it) => acc + it.transaccion.montoTotal, 0),
    [items],
  );

  const tone = FILTER_TONE[tipoFilter];
  const toneText =
    tone === 'success'
      ? 'text-success'
      : tone === 'warning'
        ? 'text-warning'
        : tone === 'danger'
          ? 'text-danger'
          : 'text-ink-strong';

  const countText = `${items.length}${hasMore ? '+' : ''} · página ${page + 1} · lote ${pageSize}`;

  return (
    <View className="flex-1 bg-canvas">
      <View className="bg-canvas px-3 pt-2">
        <SearchBar
          value={search}
          onChangeText={handleSearchChange}
          placeholder={`Buscar en ${FILTERS_INFO[tipoFilter]}…`}
        />
        <TipoFilterChips value={tipoFilter} onChange={handleTipoChange} />
      </View>

      {items.length > 0 ? (
        <View className="mx-3 mb-1 flex-row items-center justify-between rounded-xl bg-surface px-3 py-2">
          <View className="flex-row items-center gap-2">
            <View className="h-2 w-2 rounded-full bg-surface-hi" />
            <Text className="text-xs font-medium text-ink">
              {countText}
            </Text>
          </View>
          <Text className={cn('text-sm font-bold tabular-nums', toneText)}>
            {format(totalFiltrado)}
          </Text>
        </View>
      ) : null}

      {loading && items.length === 0 ? (
        <MovimientosSkeleton />
      ) : (
        <FlashList
          data={items}
          keyExtractor={keyExtractor}
          renderItem={renderItem}
          onEndReached={onEndReached}
          onEndReachedThreshold={0.5}
          refreshControl={
            <RefreshControl
              refreshing={loading && items.length > 0}
              onRefresh={() => { setPage(0); refresh(); }}
              tintColor={DARK_PALETTE.accentBright}
              colors={[DARK_PALETTE.accentBright]}
            />
          }
          contentContainerClassName="pb-4"
          ListEmptyComponent={
            <EmptyState
              icon="document-text-outline"
              title="Sin movimientos"
              description={
                search
                  ? `No hay "${search}" en ${FILTERS_INFO[tipoFilter]}.`
                  : `Aún no hay ${FILTERS_INFO[tipoFilter] === 'todos los tipos' ? 'movimientos' : FILTERS_INFO[tipoFilter]}. Registra tu primera transacción en la pestaña Registro.`
              }
              action={
                search ? null : (
                  <AnimatedPressable
                    onPress={() => router.push('/(tabs)/registro')}
                    accessibilityRole="button"
                    accessibilityLabel="Ir a registro para crear una transacción"
                    className="min-h-[44px] flex-row items-center justify-center rounded-xl bg-accent px-4 py-2.5"
                  >
                    <Text className="text-base font-bold text-onAccent">Ir a Registro</Text>
                  </AnimatedPressable>
                )
              }
            />
          }
          ListFooterComponent={hasMore && loading ? <ListFooterLoader count={3} itemHeight={84} /> : null}
        />
      )}
    </View>
  );
}
