import React, { useCallback, useMemo, useState } from 'react';
import { FlashList } from '@shopify/flash-list';
import { router, Stack } from 'expo-router';
import { Pressable, Text, View } from 'react-native';

import { SearchBar } from '@/presentation/components/filters/SearchBar';
import { SortMenu } from '@/presentation/components/filters/SortMenu';
import { CategoryChipsRow } from '@/presentation/components/filters/CategoryChipsRow';
import { ProductGridCard } from '@/presentation/components/product/ProductGridCard';
import { EmptyState } from '@/presentation/components/feedback/EmptyState';
import { useProductos } from '@/presentation/hooks/useProductos';
import { useCategorias } from '@/presentation/hooks/useCategorias';
import { useDebouncedValue, useInfiniteScroll } from '@/presentation/hooks/useInfiniteScroll';
import type { SortOrder } from '@/data/repositories/IProductoRepository';

const ESTIMATED_ITEM = 180;

export default function ProductosScreen() {
  const [search, setSearch] = useState('');
  const [categoriaId, setCategoriaId] = useState<number | null>(null);
  const [sort, setSort] = useState<SortOrder>('POPULARIDAD_DESC');
  const [page, setPage] = useState(0);
  const debouncedSearch = useDebouncedValue(search, 200);

  const { categorias } = useCategorias();
  const { items, hasMore, refresh } = useProductos({
    search: debouncedSearch,
    categoriaId: categoriaId ?? undefined,
    sort,
    page,
  });

  const handleCategoriaSelect = useCallback((id: number | null) => {
    setCategoriaId(id);
    setPage(0);
  }, []);

  const handleSearchChange = useCallback((v: string) => {
    setSearch(v);
    setPage(0);
  }, []);

  const handleSortChange = useCallback((v: SortOrder) => {
    setSort(v);
    setPage(0);
  }, []);

  const handlePress = useCallback((id: number) => {
    router.push(`/productos/${id}`);
  }, []);

  const onEndReached = useInfiniteScroll(() => setPage((p) => p + 1), hasMore, false);

  const data = useMemo(() => items, [items]);

  return (
    <View className="flex-1 bg-surface-50 dark:bg-surface-950">
      <Stack.Screen options={{ title: 'Productos' }} />
      <View className="p-3">
        <View className="mb-2 flex-row gap-2">
          <View className="flex-1">
            <SearchBar value={search} onChangeText={handleSearchChange} placeholder="Buscar productos…" />
          </View>
          <SortMenu value={sort} onChange={handleSortChange} />
        </View>
        <CategoryChipsRow
          categorias={categorias}
          selectedId={categoriaId}
          onSelect={handleCategoriaSelect}
        />
      </View>

      <FlashList
        data={data}
        numColumns={3}
        keyExtractor={(item) => String(item.id)}
        renderItem={({ item }) => (
          <ProductGridCard producto={item} onPress={handlePress} />
        )}
        onEndReached={onEndReached}
        onEndReachedThreshold={0.5}
        contentContainerClassName="px-2 pb-24"
        ListEmptyComponent={
          <EmptyState
            title="Sin productos"
            description="Crea tu primer producto para empezar a vender."
          />
        }
      />

      <Pressable
        onPress={() => router.push('/productos/nuevo')}
        accessibilityLabel="Crear producto"
        className="absolute bottom-6 right-6 h-14 w-14 items-center justify-center rounded-full bg-primary-600 shadow-lg active:bg-primary-700"
        style={{ elevation: 6 }}
      >
        <Text className="text-3xl font-bold text-white">+</Text>
      </Pressable>
    </View>
  );
}
