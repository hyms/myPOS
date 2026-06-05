import React, { useCallback, useState } from 'react';
import { Modal, Pressable, Text, TextInput, View } from 'react-native';

import { getRepositories } from '@/data/repositories/container';
import { useProductoDraftStore } from '@/presentation/stores/productoDraftStore';
import { Button } from '@/presentation/components/ui/Button';
import { Input } from '@/presentation/components/ui/Input';
import { ToastService } from '@/infrastructure/toast/ToastService';
import { useInvalidationStore } from '@/presentation/stores/invalidationStore';
import { cn } from '@/shared/utils/cn';

interface Props {
  readonly visible: boolean;
  readonly onClose: () => void;
  readonly onCreated?: (id: number, nombre: string) => void;
}

export function QuickCategoryModal({ visible, onClose, onCreated }: Props) {
  const setCategorias = useProductoDraftStore((s) => s.setCategorias);
  const invalidateMany = useInvalidationStore((s) => s.invalidateMany);
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
      invalidateMany(['categorias']);
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
        <Pressable onPress={() => {}} className="w-full max-w-md rounded-2xl bg-surface p-5 bg-surface">
          <Text className="mb-3 text-lg font-bold text-ink-strong">Nueva categoría</Text>
          <Input
            label="Nombre"
            value={nombre}
            onChangeText={setNombre}
            placeholder="Ej. Bebidas"
            autoFocus
            {...(error ? { error } : {})}
            onSubmitEditing={handleSubmit}
            returnKeyType="done"
          />
          <View className={cn('mt-4 flex-row justify-end gap-2')}>
            <Pressable onPress={handleClose} className="rounded-lg px-4 py-3 active:bg-surface-lo">
              <Text className="font-semibold text-ink">Cancelar</Text>
            </Pressable>
            <Button title="Crear" onPress={handleSubmit} busy={busy} disabled={busy} />
          </View>
        </Pressable>
      </Pressable>
    </Modal>
  );
}
