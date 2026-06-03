import React, { memo } from 'react';
import { Modal, Pressable, Text, View } from 'react-native';
import { useSafeAreaInsets } from 'react-native-safe-area-context';

import { cn } from '@/shared/utils/cn';

interface ModalSheetProps {
  readonly visible: boolean;
  readonly onClose: () => void;
  readonly title: string;
  readonly children: React.ReactNode;
  readonly footer?: React.ReactNode;
}

function ModalSheetComponent({ visible, onClose, title, children, footer }: ModalSheetProps) {
  const insets = useSafeAreaInsets();
  return (
    <Modal
      transparent
      animationType="slide"
      visible={visible}
      onRequestClose={onClose}
      statusBarTranslucent
    >
      <Pressable
        accessibilityLabel="Cerrar"
        onPress={onClose}
        className="flex-1 bg-black/40"
      >
        <View className="flex-1" />
      </Pressable>
      <View
        className={cn(
          'rounded-t-3xl bg-white px-5 pb-6 pt-4 dark:bg-surface-900',
          'shadow-2xl',
        )}
        style={{ paddingBottom: 24 + insets.bottom }}
      >
        <View className="mb-3 flex-row items-center justify-between">
          <Text className="text-lg font-bold text-surface-900 dark:text-surface-50">{title}</Text>
          <Pressable onPress={onClose} className="rounded-full p-1 active:bg-surface-200">
            <Text className="text-2xl text-surface-500">×</Text>
          </Pressable>
        </View>
        <View>{children}</View>
        {footer ? <View className="mt-4">{footer}</View> : null}
      </View>
    </Modal>
  );
}

export const ModalSheet = memo(ModalSheetComponent);
