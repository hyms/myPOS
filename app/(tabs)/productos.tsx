import React, { useCallback, useMemo, useState } from 'react';
import { FlashList } from '@shopify/flash-list';
import type { ListRenderItem } from '@shopify/flash-list';
import { router, Stack } from 'expo-router';
import { View } from 'react-native';

import { SearchBar } from '@/presentation/components/filters/SearchBar';
import { SortMenu } from '@/presentation/components/filters/SortMenu';
import { CategoryChipsRow } from '@/presentation/components/filters/CategoryChipsRow';
import { ProductGridCard } from '@/presentation/components/product/ProductGridCard';
import { Skeleton } from '@/presentation/components/ui/Skeleton';
import { AnimatedPressable } from '@/presentation/components/ui/AnimatedPressable';
import { Icon } from '@/presentation/components/ui/Icon';
import { EmptyState } from '@/presentation/components/feedback/EmptyState';
import { useProductos } from '@/presentation/hooks/useProductos';
import { useCategorias } from '@/presentation/hooks/useCategorias';
import { useDebouncedValue, useInfiniteScroll } from '@/presentation/hooks/useInfiniteScroll';
import type { ProductoConPopularidad } from '@/presentation/hooks/useProductos';
import type { SortOrder } from '@/data/repositories/IProductoRepository';

function ProductSkeleton() {
  return (
    <View className="m-1 flex-1 overflow-hidden rounded-2xl border border-surface-100 bg-white dark:border-surface-800 dark:bg-surface-900">
      <Skeleton className="aspect-square w-full rounded-none" />
      <View className="gap-1.5 p-2">
        <Skeleton className="h-3.5 w-3/4" />
        <Skeleton className="h-3.5 w-1/2" />
        <Skeleton className="h-3 w-1/3" />
      </View>
    </View>
  );
}

interface RenderProductProps {
  readonly item: ProductoConPopularidad;
  readonly onPress: (id: number) => void;
}

const RenderProduct = React.memo(function RenderProduct({ item, onPress }: RenderProductProps) {
  return <ProductGridCard producto={item} onPress={onPress} />;
});

export default function ProductosScreen() {
  const [search, setSearch] = useState('');
  const [categoriaId, setCategoriaId] = useState<number | null>(null);
  const [sort, setSort] = useState<SortOrder>('POPULARIDAD_DESC');
  const [page, setPage] = useState(0);
  const debouncedSearch = useDebouncedValue(search, 200);

  const { categorias } = useCategorias();
  const { items, hasMore, loading, refresh } = useProductos({
    search: debouncedSearch, categoriaId: categoriaId ?? undefined, sort, page,
  });

  const handleCategoriaSelect = useCallback((id: number | null) => { setCategoriaId(id); setPage(0); }, []);
  const handleSearchChange = useCallback((v: string) => { setSearch(v); setPage(0); }, []);
  const handleSortChange = useCallback((v: SortOrder) => { setSort(v); setPage(0); }, []);
  const handlePress = useCallback((id: number) => { router.push(`/productos/${id}`); }, []);
  const handleNewPress = useCallback(() => { router.push('/productos/nuevo'); }, []);
  const { onEndReached } = useInfiniteScroll(() => setPage((p) => p + 1), hasMore, false);
  const data = useMemo(() => items, [items]);

  const renderItem: ListRenderItem<ProductoConPopularidad> = useCallback(
    ({ item }) => <RenderProduct item={item} onPress={handlePress} />,
    [handlePress],
  );

  const keyExtractor = useCallback((item: ProductoConPopularidad) => String(item.id), []);

  return (
    <View className="flex-1 bg-surface-50 dark:bg-surface-950">
      <Stack.Screen options={{ title: 'Productos' }} />
      <View className="p-3">
        <View className="mb-2 flex-row gap-2">
          <View className="flex-1"><SearchBar value={search} onChangeText={handleSearchChange} placeholder="Buscar productos…" /></View>
          <SortMenu value={sort} onChange={handleSortChange} />
        </View>
        <CategoryChipsRow categorias={categorias} selectedId={categoriaId} onSelect={handleCategoriaSelect} />
      </View>
      {loading && items.length === 0 ? (
        <View className="flex-row flex-wrap px-2">
          {Array.from({ length: 6 }).map((_, i) => <ProductSkeleton key={`skel-${i}`} />)}
        </View>
      ) : (
        <FlashList
          data={data}
          numColumns={3}
          keyExtractor={keyExtractor}
          renderItem={renderItem}
          onEndReached={onEndReached}
          onEndReachedThreshold={0.5}
          contentContainerClassName="px-2 pb-24"
          ListEmptyComponent={<EmptyState icon="cube-outline" title="Sin productos" description="Crea tu primer producto para empezar a vender." />}
        />
      )}
      <AnimatedPressable
        onPress={handleNewPress}
        accessibilityLabel="Crear producto"
        className="absolute bottom-6 right-6 h-14 w-14 items-center justify-center rounded-full bg-primary-600 shadow-lg"
        style={{ elevation: 6 }}
      >
        <Icon name="add" size={28} color="#fff" />
      </AnimatedPressable>
    </View>
  );
}
