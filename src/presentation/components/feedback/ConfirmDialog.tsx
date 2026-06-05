import React from 'react';
import { Pressable, Text, View } from 'react-native';

import { Button } from '../ui/Button';

interface ConfirmDialogProps {
  readonly visible: boolean;
  readonly title: string;
  readonly message: string;
  readonly confirmLabel?: string;
  readonly cancelLabel?: string;
  readonly destructive?: boolean;
  readonly onConfirm: () => void;
  readonly onCancel: () => void;
  readonly busy?: boolean;
}

export function ConfirmDialog({
  visible,
  title,
  message,
  confirmLabel = 'Confirmar',
  cancelLabel = 'Cancelar',
  destructive,
  onConfirm,
  onCancel,
  busy,
}: ConfirmDialogProps) {
  if (!visible) return null;
  return (
    <View className="absolute inset-0 z-50 items-center justify-center bg-black/50 px-6">
      <View className="w-full max-w-md rounded-2xl bg-white p-5 dark:bg-surface-900">
        <Text className="text-lg font-bold text-surface-900 dark:text-surface-50">{title}</Text>
        <Text className="mt-2 text-base text-surface-700 dark:text-surface-200">{message}</Text>
        <View className="mt-5 flex-row justify-end gap-3">
          <Pressable
            onPress={onCancel}
            className="rounded-lg px-4 py-3 active:bg-surface-200"
            accessibilityRole="button"
          >
            <Text className="font-semibold text-surface-700 dark:text-surface-200">{cancelLabel}</Text>
          </Pressable>
          <Button
            title={confirmLabel}
            variant={destructive ? 'danger' : 'primary'}
            onPress={onConfirm}
            disabled={busy}
          />
        </View>
      </View>
    </View>
  );
}
