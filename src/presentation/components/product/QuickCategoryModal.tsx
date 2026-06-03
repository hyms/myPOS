import React, { useCallback, useState } from 'react';
import { Modal, Pressable, Text, TextInput, View } from 'react-native';

import { getRepositories } from '@/data/repositories/container';
import { useProductoDraftStore } from '@/presentation/stores/productoDraftStore';
import { Button } from '@/presentation/components/ui/Button';
import { Input } from '@/presentation/components/ui/Input';
import { ToastService } from '@/infrastructure/toast/ToastService';
import { cn } from '@/shared/utils/cn';

interface Props {
  readonly visible: boolean;
  readonly onClose: () => void;
  readonly onCreated?: (id: number, nombre: string) => void;
}

export function QuickCategoryModal({ visible, onClose, onCreated }: Props) {
  const setCategorias = useProductoDraftStore((s) => s.setCategorias);
  const [nombre, setNombre] = useState('');
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const reset = useCallback(() => {
    setNombre('');
    setError(null);
    setBusy(false);
  }, []);

  const handleClose = useCallback(() => {
    reset();
    onClose();
  }, [onClose, reset]);

  const handleSubmit = useCallback(async () => {
    if (!nombre.trim()) {
      setError('El nombre es obligatorio');
      return;
    }
    setBusy(true);
    try {
      const repo = getRepositories().categorias;
      const created = repo.crear({ nombre });
      const list = repo.listar();
      setCategorias(list);
      ToastService.success('Categoría creada', created.nombre);
      onCreated?.(created.id, created.nombre);
      reset();
      onClose();
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Error desconocido');
    } finally {
      setBusy(false);
    }
  }, [nombre, onClose, onCreated, reset, setCategorias]);

  return (
    <Modal transparent animationType="fade" visible={visible} onRequestClose={handleClose}>
      <Pressable onPress={handleClose} className="flex-1 items-center justify-center bg-black/50 px-6">
        <Pressable onPress={() => {}} className="w-full max-w-md rounded-2xl bg-white p-5 dark:bg-surface-900">
          <Text className="mb-3 text-lg font-bold text-surface-900 dark:text-surface-50">Nueva categoría</Text>
          <Input
            label="Nombre"
            value={nombre}
            onChangeText={setNombre}
            placeholder="Ej. Bebidas"
            autoFocus
            error={error ?? undefined}
            onSubmitEditing={handleSubmit}
            returnKeyType="done"
          />
          <View className={cn('mt-4 flex-row justify-end gap-2')}>
            <Pressable onPress={handleClose} className="rounded-lg px-4 py-3 active:bg-surface-200">
              <Text className="font-semibold text-surface-700 dark:text-surface-200">Cancelar</Text>
            </Pressable>
            <Button title="Crear" onPress={handleSubmit} busy={busy} disabled={busy} />
          </View>
        </Pressable>
      </Pressable>
    </Modal>
  );
}
