import React, { useCallback, useState } from 'react';
import { Alert, Pressable, Text, View } from 'react-native';
import { FlashList } from '@shopify/flash-list';
import { router } from 'expo-router';

import type { CarritoTipo, CarritoItem } from '@/domain/entities/CarritoItem';
import { registrarVenta } from '@/application/ventas/RegistrarVenta';
import { registrarCompra } from '@/application/compras/RegistrarCompra';
import { useCarritoStore } from '@/presentation/stores/carritoStore';
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

interface Props {
  readonly tipo: CarritoTipo;
}

export function CarritoScreen({ tipo }: Props) {
  const setTipo = useCarritoStore((s) => s.setTipo);
  const items = useCarritoStore((s) => s.items);
  const agregar = useCarritoStore((s) => s.agregar);
  const eliminar = useCarritoStore((s) => s.eliminar);
  const vaciar = useCarritoStore((s) => s.vaciar);
  const totals = useCarritoTotals();
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

  useCarritoTypeGuard(tipo, setTipo);

  const { items: productos, hasMore } = useProductos({
    search: debouncedSearch,
    categoriaId: categoriaId ?? undefined,
    sort,
    page,
  });

  const handlePress = useCallback(
    (id: number) => {
      const p = productos.find((x) => x.id === id);
      if (!p) return;
      if (tipo === 'VENTA' && p.stockActual <= 0) {
        ToastService.warning('Sin stock', p.nombre);
        return;
      }
      agregar(p, 1);
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

  const onEndReached = useInfiniteScroll(() => setPage((p) => p + 1), hasMore, false);

  const handleConfirm = useCallback(
    async ({ tipoPago, recibido }: { tipoPago: TipoPago; recibido: number | null }) => {
      try {
        if (tipo === 'VENTA') {
          const result = registrarVenta({
            items: useCarritoStore.getState().items,
            tipoPago,
            detalle: null,
          });
          setPaymentOpen(false);
          ToastService.success('Venta registrada', `Total ${format(result.total)}`);
          stockAlert.notify(result.stockAlerts);
          await generarYCompartirComprobante({
            tipo: 'VENTA',
            transaccionId: result.transaccionId,
            items: useCarritoStore.getState().items,
            tipoPago,
            recibido,
            cambio: recibido !== null ? recibido - result.total : null,
            currency,
          });
        } else {
          const result = registrarCompra({
            items: useCarritoStore.getState().items,
            tipoPago,
            detalle: null,
          });
          setPaymentOpen(false);
          ToastService.success('Compra registrada', `Total ${format(result.total)}`);
          await generarYCompartirComprobante({
            tipo: 'COMPRA',
            transaccionId: result.transaccionId,
            items: useCarritoStore.getState().items,
            tipoPago,
            currency,
          });
        }
        vaciar();
        setPage(0);
      } catch (e) {
        ToastService.error('Error', e instanceof Error ? e.message : 'No se pudo registrar.');
      }
    },
    [currency, format, stockAlert, tipo, vaciar],
  );

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
          <Text className="mb-1 text-xs font-semibold uppercase text-surface-500">
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
                <Text className="text-sm font-semibold text-surface-900 dark:text-surface-50" numberOfLines={1}>
                  {it.producto.nombre}
                </Text>
                <Text className="text-xs text-surface-500">
                  {it.cantidad} × {format(tipo === 'VENTA' ? it.producto.precioVenta : it.producto.precioCompra)}
                  {it.descuento > 0 ? `  ·  desc ${format(it.descuento)}` : ''}
                </Text>
              </View>
              <Text className="font-bold text-primary-700">
                {format(
                  (tipo === 'VENTA' ? it.producto.precioVenta : it.producto.precioCompra) * it.cantidad -
                    it.descuento,
                )}
              </Text>
            </Pressable>
          ))}
          <View className="mt-2 flex-row justify-between">
            <Text className="text-sm text-surface-600">Subtotal</Text>
            <Text className="font-semibold text-surface-900 dark:text-surface-50">{format(totals.subtotal)}</Text>
          </View>
          {totals.descuento > 0 ? (
            <View className="flex-row justify-between">
              <Text className="text-sm text-surface-600">Descuento</Text>
              <Text className="font-semibold text-danger-700">-{format(totals.descuento)}</Text>
            </View>
          ) : null}
          <View className="mt-1 flex-row justify-between">
            <Text className="text-base font-bold text-surface-900 dark:text-surface-50">Total</Text>
            <Text className="text-lg font-bold text-primary-700">{format(totals.total)}</Text>
          </View>
        </View>
      ) : null}

      <FlashList
        data={productos}
        numColumns={3}
        keyExtractor={(item) => String(item.id)}
        renderItem={({ item }) => (
          <ProductGridCard
            producto={item}
            onPress={handlePress}
          />
        )}
        onEndReached={onEndReached}
        onEndReachedThreshold={0.5}
        contentContainerClassName="px-2 pb-32"
        ListEmptyComponent={
          <EmptyState title="Sin productos" description="Ajusta filtros o crea productos nuevos." />
        }
      />

      <CartSummaryBar onCheckout={() => setPaymentOpen(true)} />

      <LineItemModal
        visible={!!lineItem}
        item={lineItem}
        onClose={() => setLineItem(null)}
      />
      <PaymentModal
        visible={paymentOpen}
        items={items}
        tipo={tipo}
        onClose={() => setPaymentOpen(false)}
        onConfirm={handleConfirm}
      />

      {items.length > 0 ? (
        <Pressable
          onPress={() => {
            Alert.alert('Vaciar carrito', '¿Eliminar todos los productos del carrito?', [
              { text: 'Cancelar', style: 'cancel' },
              { text: 'Vaciar', style: 'destructive', onPress: () => vaciar() },
            ]);
          }}
          className="absolute bottom-28 left-3 rounded-full bg-danger-600 px-3 py-1.5 active:bg-danger-700"
        >
          <Text className="text-xs font-bold text-white">Vaciar</Text>
        </Pressable>
      ) : null}
    </View>
  );
}

function useCarritoTypeGuard(tipo: CarritoTipo, setTipo: (t: CarritoTipo) => void) {
  React.useEffect(() => {
    if (useCarritoStore.getState().tipo !== tipo) {
      setTipo(tipo);
    }
  }, [tipo, setTipo]);
}
