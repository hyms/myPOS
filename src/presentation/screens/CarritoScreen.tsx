import React, { useCallback, useState } from 'react';
import { Alert, Pressable, Text, View } from 'react-native';
import { FlashList } from '@shopify/flash-list';
import type { ListRenderItem } from '@shopify/flash-list';
import { router } from 'expo-router';
import * as Haptics from 'expo-haptics';

import type { CarritoTipo, CarritoItem } from '@/domain/entities/CarritoItem';
import { registrarVenta } from '@/application/ventas/RegistrarVenta';
import { registrarCompra } from '@/application/compras/RegistrarCompra';
import type { CarritoStoreHook } from '@/presentation/stores/carritoStore';
import { useProductos } from '@/presentation/hooks/useProductos';
import { useCategorias } from '@/presentation/hooks/useCategorias';
import { useStockAlert } from '@/presentation/hooks/useStockAlert';
import { useCarritoTotals } from '@/presentation/hooks/useCarrito';
import { useCurrency } from '@/presentation/hooks/useCurrency';
import { useDebouncedValue, useInfiniteScroll } from '@/presentation/hooks/useInfiniteScroll';
import { SearchBar } from '@/presentation/components/filters/SearchBar';
import { SortMenu } from '@/presentation/components/filters/SortMenu';
import { CategoryChipsRow } from '@/presentation/components/filters/CategoryChipsRow';
import { ProductGridCard } from '@/presentation/components/product/ProductGridCard';
import { LineItemModal } from '@/presentation/components/cart/LineItemModal';
import { PaymentModal } from '@/presentation/components/cart/PaymentModal';
import { CartSummaryBar } from '@/presentation/components/cart/CartSummaryBar';
import { EmptyState } from '@/presentation/components/feedback/EmptyState';
import { generarYCompartirComprobante } from '@/infrastructure/pdf/ComprobanteService';
import { useSettingsStore } from '@/presentation/stores/settingsStore';
import type { SortOrder } from '@/data/repositories/IProductoRepository';
import { ToastService } from '@/infrastructure/toast/ToastService';
import type { TipoPago } from '@/domain/entities/Transaccion';
import type { ProductoConPopularidad } from '@/presentation/hooks/useProductos';

interface RenderProductProps {
  readonly item: ProductoConPopularidad;
  readonly onPress: (id: number) => void;
}

const RenderProduct = React.memo(function RenderProduct({ item, onPress }: RenderProductProps) {
  return <ProductGridCard producto={item} onPress={onPress} />;
});

interface Props {
  readonly tipo: CarritoTipo;
  readonly store: CarritoStoreHook;
}

export function CarritoScreen({ tipo, store }: Props) {
  const items = store((s) => s.items);
  const agregar = store((s) => s.agregar);
  const eliminar = store((s) => s.eliminar);
  const vaciar = store((s) => s.vaciar);
  const totals = useCarritoTotals(store, tipo);
  const { format } = useCurrency();
  const currency = useSettingsStore((s) => s.currency);
  const stockAlert = useStockAlert();
  const { categorias } = useCategorias();

  const [search, setSearch] = useState('');
  const [categoriaId, setCategoriaId] = useState<number | null>(null);
  const [sort, setSort] = useState<SortOrder>('POPULARIDAD_DESC');
  const [page, setPage] = useState(0);
  const [lineItem, setLineItem] = useState<CarritoItem | null>(null);
  const [paymentOpen, setPaymentOpen] = useState(false);
  const debouncedSearch = useDebouncedValue(search, 200);

  const { items: productos, hasMore } = useProductos({
    search: debouncedSearch,
    ...(categoriaId !== null ? { categoriaId } : {}),
    sort,
    page,
  });

  const handlePress = useCallback(
    (id: number) => {
      const p = productos.find((x) => x.id === id);
      if (!p) return;
      if (tipo === 'VENTA' && p.stockActual <= 0) {
        ToastService.warning('Sin stock', p.nombre);
        Haptics.notificationAsync(Haptics.NotificationFeedbackType.Warning).catch(() => {});
        return;
      }
      agregar(p, 1);
      Haptics.impactAsync(Haptics.ImpactFeedbackStyle.Light).catch(() => {});
    },
    [agregar, productos, tipo],
  );

  const handleLongPress = useCallback(
    (id: number) => {
      const it = items.find((i) => i.producto.id === id);
      if (it) setLineItem(it);
    },
    [items],
  );

  const handleSearchChange = useCallback((v: string) => {
    setSearch(v);
    setPage(0);
  }, []);

  const handleCategoriaSelect = useCallback((id: number | null) => {
    setCategoriaId(id);
    setPage(0);
  }, []);

  const handleSortChange = useCallback((v: SortOrder) => {
    setSort(v);
    setPage(0);
  }, []);

  const { onEndReached } = useInfiniteScroll(() => setPage((p) => p + 1), hasMore, false);

  const renderItem: ListRenderItem<ProductoConPopularidad> = useCallback(
    ({ item }) => <RenderProduct item={item} onPress={handlePress} />,
    [handlePress],
  );

  const keyExtractor = useCallback((item: ProductoConPopularidad) => String(item.id), []);

  const handleConfirm = useCallback(
    async ({ tipoPago, recibido }: { tipoPago: TipoPago; recibido: number | null }) => {
      try {
        const currentItems = store.getState().items;
        let comprobanteData: Parameters<typeof generarYCompartirComprobante>[0];

        if (tipo === 'VENTA') {
          const result = registrarVenta({
            items: currentItems,
            tipoPago,
            detalle: null,
          });
          stockAlert.notify(result.stockAlerts);
          Haptics.notificationAsync(Haptics.NotificationFeedbackType.Success).catch(() => {});
          ToastService.success('Venta registrada', `Total ${format(result.total)}`);
          comprobanteData = {
            tipo: 'VENTA',
            transaccionId: result.transaccionId,
            items: currentItems,
            tipoPago,
            recibido,
            cambio: recibido !== null ? recibido - result.total : null,
            currency,
          };
        } else {
          const result = registrarCompra({
            items: currentItems,
            tipoPago,
            detalle: null,
          });
          Haptics.notificationAsync(Haptics.NotificationFeedbackType.Success).catch(() => {});
          ToastService.success('Compra registrada', `Total ${format(result.total)}`);
          comprobanteData = {
            tipo: 'COMPRA',
            transaccionId: result.transaccionId,
            items: currentItems,
            tipoPago,
            currency,
          };
        }

        setPaymentOpen(false);
        vaciar();
        setPage(0);

        void generarYCompartirComprobante(comprobanteData).catch((e) => {
          ToastService.warning(
            'Comprobante no generado',
            e instanceof Error ? e.message : 'No se pudo generar el PDF.',
          );
        });
      } catch (e) {
        ToastService.error('Error', e instanceof Error ? e.message : 'No se pudo registrar.');
      }
    },
    [currency, format, stockAlert, store, tipo, vaciar],
  );

  const handleOpenPayment = useCallback(() => setPaymentOpen(true), []);
  const handleClosePayment = useCallback(() => setPaymentOpen(false), []);
  const handleCloseLineItem = useCallback(() => setLineItem(null), []);

  const handleClearCart = useCallback(() => {
    Alert.alert('Vaciar carrito', '¿Eliminar todos los productos del carrito?', [
      { text: 'Cancelar', style: 'cancel' },
      { text: 'Vaciar', style: 'destructive', onPress: () => vaciar() },
    ]);
  }, [vaciar]);

  return (
    <View className="flex-1 bg-surface-50 dark:bg-surface-950">
      <View className="p-3">
        <View className="mb-2 flex-row gap-2">
          <View className="flex-1">
            <SearchBar
              value={search}
              onChangeText={handleSearchChange}
              placeholder={tipo === 'VENTA' ? 'Buscar para vender…' : 'Buscar para comprar…'}
            />
          </View>
          <SortMenu value={sort} onChange={handleSortChange} />
        </View>
        <CategoryChipsRow
          categorias={categorias}
          selectedId={categoriaId}
          onSelect={handleCategoriaSelect}
        />
      </View>

      {items.length > 0 ? (
        <View className="border-y border-surface-200 bg-white px-3 py-2 dark:border-surface-800 dark:bg-surface-900">
          <Text
            className="mb-1 text-xs font-semibold uppercase"
            style={{ color: '#64748b' }}
          >
            Carrito {tipo === 'VENTA' ? 'de venta' : 'de compra'}
          </Text>
          {items.map((it) => (
            <Pressable
              key={it.producto.id}
              onPress={() => setLineItem(it)}
              onLongPress={() => eliminar(it.producto.id)}
              className="flex-row items-center justify-between border-b border-surface-100 py-2 active:bg-surface-50 dark:border-surface-800"
            >
              <View className="flex-1 pr-3">
                <Text
                  className="text-sm font-semibold"
                  numberOfLines={1}
                  style={{ color: '#0f172a' }}
                >
                  {it.producto.nombre}
                </Text>
                <Text className="text-xs" style={{ color: '#64748b' }}>
                  {it.cantidad} × {format(tipo === 'VENTA' ? it.producto.precioVenta : it.producto.precioCompra)}
                  {it.descuento > 0 ? `  ·  desc ${format(it.descuento)}` : ''}
                </Text>
              </View>
              <Text className="font-bold" style={{ color: '#b45309' }}>
                {format(
                  (tipo === 'VENTA' ? it.producto.precioVenta : it.producto.precioCompra) * it.cantidad -
                    it.descuento,
                )}
              </Text>
            </Pressable>
          ))}
          <View className="mt-2 flex-row justify-between">
            <Text className="text-sm" style={{ color: '#475569' }}>Subtotal</Text>
            <Text className="font-semibold" style={{ color: '#0f172a' }}>{format(totals.subtotal)}</Text>
          </View>
          {totals.descuento > 0 ? (
            <View className="flex-row justify-between">
              <Text className="text-sm" style={{ color: '#475569' }}>Descuento</Text>
              <Text className="font-semibold" style={{ color: '#b91c1c' }}>-{format(totals.descuento)}</Text>
            </View>
          ) : null}
          <View className="mt-1 flex-row justify-between">
            <Text className="text-base font-bold" style={{ color: '#0f172a' }}>Total</Text>
            <Text className="text-lg font-bold" style={{ color: '#b45309' }}>{format(totals.total)}</Text>
          </View>
        </View>
      ) : null}

      <FlashList
        data={productos}
        numColumns={3}
        keyExtractor={keyExtractor}
        renderItem={renderItem}
        onEndReached={onEndReached}
        onEndReachedThreshold={0.5}
        contentContainerClassName="px-2 pb-32"
        ListEmptyComponent={
          <EmptyState icon="cube-outline" title="Sin productos" description="Ajusta filtros o crea productos nuevos." />
        }
      />

      <CartSummaryBar store={store} tipo={tipo} onCheckout={handleOpenPayment} />

      <LineItemModal
        visible={!!lineItem}
        item={lineItem}
        store={store}
        tipo={tipo}
        onClose={handleCloseLineItem}
      />
      <PaymentModal
        visible={paymentOpen}
        items={items}
        tipo={tipo}
        onClose={handleClosePayment}
        onConfirm={handleConfirm}
      />

      {items.length > 0 ? (
        <Pressable
          onPress={handleClearCart}
          className="absolute bottom-28 left-3 rounded-full bg-danger-600 px-3 py-1.5 active:bg-danger-700"
        >
          <Text className="text-xs font-bold text-white">Vaciar</Text>
        </Pressable>
      ) : null}
    </View>
  );
}
