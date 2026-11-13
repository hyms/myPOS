import React, { memo, useEffect } from 'react';
import { Modal, Pressable, Text, View } from 'react-native';
import Animated, {
  Easing,
  useAnimatedStyle,
  useSharedValue,
  withTiming,
} from 'react-native-reanimated';
import { useSafeAreaInsets } from 'react-native-safe-area-context';

import { Icon } from '@/presentation/components/ui/Icon';
import { useReducedMotion } from '@/presentation/hooks/useReducedMotion';
import { cn } from '@/shared/utils/cn';
import { DARK_PALETTE } from '@/presentation/theme/tokens';

interface ModalSheetProps {
  readonly visible: boolean;
  readonly onClose: () => void;
  readonly title: string;
  readonly children: React.ReactNode;
  readonly footer?: React.ReactNode;
}

const ENTER_DURATION = 280;
const EASING_OUT = Easing.out(Easing.quad);
const HIDDEN_OFFSET = 600;

function ModalSheetComponent({ visible, onClose, title, children, footer }: ModalSheetProps) {
  const insets = useSafeAreaInsets();
  const reduced = useReducedMotion();
  const duration = reduced ? 0 : ENTER_DURATION;
  const translateY = useSharedValue(HIDDEN_OFFSET);
  const opacity = useSharedValue(0);

  useEffect(() => {
    if (visible) {
      translateY.value = withTiming(0, { duration, easing: EASING_OUT });
      opacity.value = withTiming(1, { duration, easing: EASING_OUT });
    } else {
      translateY.value = HIDDEN_OFFSET;
      opacity.value = 0;
    }
  }, [duration, opacity, translateY, visible]);

  const sheetStyle = useAnimatedStyle(() => ({
    transform: [{ translateY: translateY.value }],
  }));
  const scrimStyle = useAnimatedStyle(() => ({
    opacity: opacity.value,
  }));

  return (
    <Modal
      transparent
      visible={visible}
      onRequestClose={onClose}
      statusBarTranslucent
      accessibilityViewIsModal
    >
      <Animated.View
        style={scrimStyle}
        className="absolute inset-0 bg-black/70"
        pointerEvents="auto"
      >
        <Pressable
          accessibilityLabel="Cerrar modal"
          accessibilityRole="button"
          onPress={onClose}
          className="absolute inset-0"
        />
      </Animated.View>
      <Animated.View
        style={[
          sheetStyle,
          {
            position: 'absolute',
            left: 0,
            right: 0,
            bottom: 0,
            paddingBottom: 24 + insets.bottom,
            paddingTop: 8,
          },
        ]}
        className={cn(
          'rounded-t-3xl bg-surface px-5 shadow-2xl border-t border-border-subtle',
        )}
        accessibilityViewIsModal
      >
        <View
          accessibilityRole="header"
          accessibilityLabel={`${title}. Modal abierto.`}
          className="mb-1 items-center pt-1"
        >
          <View className="h-1 w-10 rounded-full bg-border" />
        </View>
        <View className="mb-3 mt-2 flex-row items-center justify-between">
          <Text
            accessibilityRole="header"
            className="text-lg font-bold text-ink-strong"
          >
            {title}
          </Text>
          <Pressable
            onPress={onClose}
            accessibilityRole="button"
            accessibilityLabel="Cerrar"
            hitSlop={12}
            className="rounded-full p-1 active:bg-surface-hi"
          >
            <Icon name="close" size={20} color={DARK_PALETTE.inkMuted} />
          </Pressable>
        </View>
        <View>{children}</View>
        {footer ? <View className="mt-4">{footer}</View> : null}
      </Animated.View>
    </Modal>
  );
}

export const ModalSheet = memo(ModalSheetComponent);
