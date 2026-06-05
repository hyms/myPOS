import React, { useCallback } from 'react';
import { Pressable, Text, View } from 'react-native';
import { Image } from 'expo-image';
import { Ionicons } from '@expo/vector-icons';
import * as ImagePicker from 'expo-image-picker';

import { optimizeProductImage } from '@/infrastructure/image/ImageOptimizer';
import { useProductoDraftStore } from '@/presentation/stores/productoDraftStore';
import { ToastService } from '@/infrastructure/toast/ToastService';
import { cn } from '@/shared/utils/cn';

interface Props {
  readonly onChange?: (uri: string | null) => void;
}

export function ProductFormHeader({ onChange }: Props) {
  const imagenUri = useProductoDraftStore((s) => s.draft.imagenUri);
  const setDraft = useProductoDraftStore((s) => s.setDraft);

  const handlePick = useCallback(
    async (source: 'camera' | 'gallery') => {
      try {
        const perm = source === 'camera'
          ? await ImagePicker.requestCameraPermissionsAsync()
          : await ImagePicker.requestMediaLibraryPermissionsAsync();
        if (!perm.granted) {
          ToastService.error('Permiso denegado', 'No se puede acceder a la fuente de imagen.');
          return;
        }
        const result = source === 'camera'
          ? await ImagePicker.launchCameraAsync({ mediaTypes: ['images'], quality: 1 })
          : await ImagePicker.launchImageLibraryAsync({ mediaTypes: ['images'], quality: 1 });
        if (result.canceled || !result.assets[0]) return;
        const optimized = await optimizeProductImage(result.assets[0].uri);
        setDraft({ imagenUri: optimized.uri });
        onChange?.(optimized.uri);
      } catch (e) {
        ToastService.error('Error', e instanceof Error ? e.message : 'No se pudo procesar la imagen.');
      }
    },
    [onChange, setDraft],
  );

  const handleClear = useCallback(() => {
    setDraft({ imagenUri: null });
    onChange?.(null);
  }, [onChange, setDraft]);

  return (
    <View className="items-center">
      <View className="h-44 w-44 overflow-hidden rounded-2xl border-2 border-dashed border-surface-300 bg-surface-50 dark:border-surface-700 dark:bg-surface-800">
        {imagenUri ? (
          <Image
            source={{ uri: imagenUri }}
            style={{ width: '100%', height: '100%' }}
            contentFit="cover"
            cachePolicy="memory-disk"
            transition={150}
          />
        ) : (
          <View className="flex-1 items-center justify-center px-3">
            <Ionicons name="camera-outline" size={48} color="#94a3b8" />
            <Text className="mt-1 text-center text-xs text-surface-500">Sin imagen</Text>
          </View>
        )}
      </View>
      <View className="mt-3 flex-row gap-2">
        <Pressable
          onPress={() => handlePick('camera')}
          className={cn(
            'rounded-lg bg-primary-600 px-4 py-2 active:bg-primary-700',
          )}
        >
          <Text className="font-semibold text-white">Cámara</Text>
        </Pressable>
        <Pressable
          onPress={() => handlePick('gallery')}
          className="rounded-lg bg-surface-200 px-4 py-2 active:bg-surface-300 dark:bg-surface-800"
        >
          <Text className="font-semibold text-surface-800 dark:text-surface-100">Galería</Text>
        </Pressable>
        {imagenUri ? (
          <Pressable
            onPress={handleClear}
            className="rounded-lg bg-danger-50 px-4 py-2 active:bg-danger-100"
          >
            <Text className="font-semibold text-danger-700">Quitar</Text>
          </Pressable>
        ) : null}
      </View>
    </View>
  );
}
