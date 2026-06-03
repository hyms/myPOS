import React, { useCallback, useEffect, useState } from 'react';
import { ScrollView, Text, View } from 'react-native';
import { router, useLocalSearchParams } from 'expo-router';

import { Button } from '@/presentation/components/ui/Button';
import { Input } from '@/presentation/components/ui/Input';
import { ProductFormHeader } from '@/presentation/components/product/ProductFormHeader';
import { CategorySelector } from '@/presentation/components/product/CategorySelector';
import { QuickCategoryModal } from '@/presentation/components/product/QuickCategoryModal';
import { useProductoDraftStore } from '@/presentation/stores/productoDraftStore';
import { useCategorias } from '@/presentation/hooks/useCategorias';
import {
  crearProducto,
  actualizarProducto,
} from '@/application/productos/GestionProductos';
import { getRepositories } from '@/data/repositories/container';
import { ToastService } from '@/infrastructure/toast/ToastService';
import { parseFloatStrict, parseIntStrict } from '@/shared/utils/format';

export default function ProductoFormScreen() {
  const params = useLocalSearchParams<{ id?: string }>();
  const editingId = params.id ? Number(params.id) : null;
  const { categorias, refresh } = useCategorias();
  const draft = useProductoDraftStore((s) => s.draft);
  const setCategorias = useProductoDraftStore((s) => s.setCategorias);
  const setDraft = useProductoDraftStore((s) => s.setDraft);
  const resetDraft = useProductoDraftStore((s) => s.resetDraft);
  const startEdit = useProductoDraftStore((s) => s.startEdit);
  const [categoriaError, setCategoriaError] = useState<string | null>(null);
  const [quickCatOpen, setQuickCatOpen] = useState(false);
  const [saving, setSaving] = useState(false);

  useEffect(() => {
    if (editingId) {
      const producto = getRepositories().productos.findById(editingId);
      if (producto) {
        startEdit(producto);
        setCategorias(getRepositories().categorias.listar());
      } else {
        ToastService.error('Producto no encontrado');
        router.back();
      }
    } else {
      resetDraft();
      refresh();
      setCategorias(getRepositories().categorias.listar());
    }
    return () => {
      resetDraft();
    };
  }, [editingId, refresh, resetDraft, setCategorias, startEdit]);

  const handleSubmit = useCallback(async () => {
    if (!draft.categoriaId) {
      setCategoriaError('La categoría es obligatoria.');
      return;
    }
    setCategoriaError(null);
    if (!draft.nombre.trim()) {
      ToastService.error('El nombre es obligatorio');
      return;
    }
    setSaving(true);
    try {
      const input = {
        categoriaId: draft.categoriaId,
        nombre: draft.nombre,
        precioVenta: parseFloatStrict(draft.precioVenta),
        precioCompra: parseFloatStrict(draft.precioCompra),
        stockActual: parseIntStrict(draft.stockActual),
        stockMinimo: parseIntStrict(draft.stockMinimo),
        imagenUri: draft.imagenUri,
      };
      if (editingId) {
        await actualizarProducto(editingId, input);
        ToastService.success('Producto actualizado');
      } else {
        await crearProducto(input);
        ToastService.success('Producto creado');
      }
      resetDraft();
      router.back();
    } catch (e) {
      ToastService.error('Error', e instanceof Error ? e.message : 'No se pudo guardar.');
    } finally {
      setSaving(false);
    }
  }, [draft, editingId, resetDraft]);

  return (
    <ScrollView
      className="flex-1 bg-surface-50 dark:bg-surface-950"
      contentContainerClassName="pb-12"
      keyboardShouldPersistTaps="handled"
    >
      <View className="items-center py-6">
        <ProductFormHeader />
      </View>

      <View className="gap-3 px-4">
        <Input
          label="Nombre"
          value={draft.nombre}
          onChangeText={(t) => setDraft({ nombre: t })}
          placeholder="Ej. Coca-Cola 600ml"
        />
        <Input
          label="Precio de venta"
          value={draft.precioVenta}
          onChangeText={(t) => setDraft({ precioVenta: t.replace(/[^0-9.]/g, '') })}
          keyboardType="decimal-pad"
          placeholder="0.00"
        />
        <Input
          label="Precio de compra"
          value={draft.precioCompra}
          onChangeText={(t) => setDraft({ precioCompra: t.replace(/[^0-9.]/g, '') })}
          keyboardType="decimal-pad"
          placeholder="0.00"
        />
        <Input
          label="Stock actual"
          value={draft.stockActual}
          onChangeText={(t) => setDraft({ stockActual: t.replace(/[^0-9]/g, '') })}
          keyboardType="number-pad"
        />
        <Input
          label="Stock mínimo"
          value={draft.stockMinimo}
          onChangeText={(t) => setDraft({ stockMinimo: t.replace(/[^0-9]/g, '') })}
          keyboardType="number-pad"
          helper="Alerta no obstructiva al pasar este umbral."
        />
        <CategorySelector
          categorias={categorias}
          onRequestNew={() => setQuickCatOpen(true)}
          error={categoriaError ?? undefined}
        />
      </View>

      <View className="mt-6 px-4">
        <Button
          title={editingId ? 'Guardar cambios' : 'Crear producto'}
          onPress={handleSubmit}
          busy={saving}
          disabled={saving}
          fullWidth
        />
      </View>

      <QuickCategoryModal
        visible={quickCatOpen}
        onClose={() => setQuickCatOpen(false)}
        onCreated={(id) => setDraft({ categoriaId: id })}
      />
    </ScrollView>
  );
}
