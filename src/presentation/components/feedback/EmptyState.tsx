import React, { useEffect } from 'react';
import { Text, View } from 'react-native';
import Animated, {
  Easing,
  useAnimatedStyle,
  useSharedValue,
  withDelay,
  withTiming,
} from 'react-native-reanimated';

import { Icon, type IconName } from '@/presentation/components/ui/Icon';
import { cn } from '@/shared/utils/cn';

interface EmptyStateProps {
  readonly title: string;
  readonly description?: string;
  readonly icon?: IconName;
  readonly className?: string;
}

const ENTER_DURATION = 320;

export function EmptyState({ title, description, icon, className }: EmptyStateProps) {
  const opacity = useSharedValue(0);
  const translateY = useSharedValue(8);

  useEffect(() => {
    opacity.value = withDelay(120, withTiming(1, { duration: ENTER_DURATION, easing: Easing.out(Easing.cubic) }));
    translateY.value = withDelay(120, withTiming(0, { duration: ENTER_DURATION, easing: Easing.out(Easing.cubic) }));
  }, [opacity, translateY]);

  const animatedStyle = useAnimatedStyle(() => ({
    opacity: opacity.value,
    transform: [{ translateY: translateY.value }],
  }));

  return (
    <Animated.View
      style={animatedStyle}
      accessibilityRole="summary"
      accessibilityLabel={`${title}. ${description ?? ''}`}
      className={cn('flex-1 items-center justify-center px-6 py-12', className)}
    >
      {icon ? (
        <View className="mb-4 rounded-full bg-surface-100 p-4 dark:bg-surface-800">
          <Icon name={icon} size={32} color="#94a3b8" />
        </View>
      ) : null}
      <Text className="text-center text-lg font-semibold text-surface-700 dark:text-surface-200">
        {title}
      </Text>
      {description ? (
        <Text className="mt-1 text-center text-sm text-surface-500">{description}</Text>
      ) : null}
    </Animated.View>
  );
}
