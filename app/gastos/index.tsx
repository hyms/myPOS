import React, { useCallback, useEffect, useState } from 'react';
import { FlashList } from '@shopify/flash-list';
import type { ListRenderItem } from '@shopify/flash-list';
import { Link } from 'expo-router';
import { RefreshControl, Text, View } from 'react-native';
import { useSafeAreaInsets } from 'react-native-safe-area-context';
import { useGastosDelMes } from '@/presentation/hooks/useTransacciones';
import { useCurrency } from '@/presentation/hooks/useCurrency';
import { useInfiniteScroll } from '@/presentation/hooks/useInfiniteScroll';
import { AnimatedPressable } from '@/presentation/components/ui/AnimatedPressable';
import { Icon } from '@/presentation/components/ui/Icon';
import { EmptyState } from '@/presentation/components/feedback/EmptyState';
import { ListFooterLoader, Skeleton } from '@/presentation/components/ui/Skeleton';
import { formatFecha } from '@/shared/utils/date';
import type { Transaccion } from '@/domain/entities/Transaccion';
import { DARK_PALETTE, SHADOW } from '@/presentation/theme/tokens';

interface GastoRowProps {
  readonly item: Transaccion;
  readonly format: (amount: number) => string;
}

const GastoRow = React.memo(function GastoRow({ item, format }: GastoRowProps) {
  return (
    <Link href={`/transacciones/${item.id}`} asChild>
      <AnimatedPressable
        accessibilityRole="button"
        accessibilityLabel={`Gasto de ${format(item.montoTotal)} con detalle ${item.detalle ?? 'sin descripción'}`}
        className="m-2 rounded-2xl border border-border-subtle bg-surface p-4"
      >
        <View className="flex-row items-center justify-between">
          <View className="flex-1 pr-2">
            <Text className="text-base font-semibold text-ink-strong" numberOfLines={2}>
              {item.detalle ?? '—'}
            </Text>
            <Text className="mt-0.5 text-xs text-ink-muted">{formatFecha(item.fechaRegistro)}</Text>
          </View>
          <View className="items-end">
            <Text className="text-lg font-bold text-danger" selectable>
              -{format(item.montoTotal)}
            </Text>
            <Text className="mt-0.5 text-[10px] uppercase tracking-wide text-ink-muted">
              {item.tipoPago}
            </Text>
          </View>
        </View>
      </AnimatedPressable>
    </Link>
  );
});

function GastoSkeleton() {
  return (
    <View className="m-2 rounded-2xl border border-border-subtle bg-surface p-4">
      <View className="flex-row items-center justify-between">
        <View className="flex-1 gap-2 pr-2">
          <Skeleton className="h-4 w-3/4" />
          <Skeleton className="h-3 w-1/2" />
        </View>
        <View className="items-end gap-2">
          <Skeleton className="h-5 w-20" />
          <Skeleton className="h-2.5 w-12" />
        </View>
      </View>
    </View>
  );
}

export default function GastosIndexScreen() {
  const insets = useSafeAreaInsets();
  const [page, setPage] = useState(0);
  const { items, hasMore, loading, refresh } = useGastosDelMes(page);
  const { format } = useCurrency();
  useEffect(() => { refresh(); }, [refresh]);
  const { onEndReached } = useInfiniteScroll(() => setPage((p) => p + 1), hasMore, loading);

  const renderItem: ListRenderItem<Transaccion> = useCallback(
    ({ item }) => <GastoRow item={item} format={format} />,
    [format],
  );

  const keyExtractor = useCallback((item: Transaccion) => String(item.id), []);

  const totalMes = items.reduce((acc, it) => acc + it.montoTotal, 0);
  const countText = `${items.length}${hasMore ? '+' : ''} gastos este mes · ${format(totalMes)}`;

  return (
    <View className="flex-1 bg-canvas">
      {items.length > 0 ? (
        <View className="border-b border-border-subtle bg-surface px-4 py-2">
          <Text
            accessibilityLabel={`Total de gastos del mes: ${format(totalMes)}`}
            className="text-[11px] font-medium text-ink-muted"
            numberOfLines={1}
          >
            {countText}
          </Text>
        </View>
      ) : null}
      {loading && items.length === 0 ? (
        <View className="pt-2">
          {Array.from({ length: 6 }).map((_, i) => <GastoSkeleton key={i} />)}
        </View>
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
          contentContainerClassName="pb-24 pt-2"
          ListEmptyComponent={
            !loading ? (
              <EmptyState
                icon="receipt-outline"
                title="Sin gastos este mes"
                description="Registra pagos de servicios, sueldos u otros gastos operativos."
                action={
                  <Link href="/gastos/nuevo" asChild>
                    <AnimatedPressable
                      accessibilityRole="button"
                      accessibilityLabel="Registrar primer gasto del mes"
                      className="min-h-[44px] flex-row items-center justify-center rounded-xl bg-danger px-4 py-2.5"
                    >
                      <Text className="text-base font-bold text-onDanger">Registrar gasto</Text>
                    </AnimatedPressable>
                  </Link>
                }
              />
            ) : null
          }
          ListFooterComponent={hasMore && loading ? <ListFooterLoader count={3} itemHeight={88} /> : null}
        />
      )}
      <Link href="/gastos/nuevo" asChild>
        <AnimatedPressable
          accessibilityRole="button"
          accessibilityLabel="Nuevo gasto"
          accessibilityHint="Abre el formulario para registrar un nuevo gasto"
          className="absolute right-6 h-14 w-14 items-center justify-center rounded-full bg-danger"
          style={[SHADOW.fab, { bottom: 24 + insets.bottom }]}
        >
          <Icon name="add" size={28} color={DARK_PALETTE.inkStrong} />
        </AnimatedPressable>
      </Link>
    </View>
  );
}
