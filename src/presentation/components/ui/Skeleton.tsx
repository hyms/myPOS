import React, { useEffect } from 'react';
import { View, type ViewStyle } from 'react-native';
import Animated, {
  useSharedValue,
  useAnimatedStyle,
  withRepeat,
  withTiming,
  Easing,
} from 'react-native-reanimated';
import { cn } from '@/shared/utils/cn';

interface SkeletonProps {
  readonly className?: string;
  readonly style?: ViewStyle;
}

const MIN_OPACITY = 0.3;
const MAX_OPACITY = 1;
const PULSE_DURATION = 800;

export function Skeleton({ className, style }: SkeletonProps) {
  const opacity = useSharedValue(MIN_OPACITY);

  useEffect(() => {
    opacity.value = withRepeat(
      withTiming(MAX_OPACITY, { duration: PULSE_DURATION, easing: Easing.inOut(Easing.quad) }),
      -1,
      true,
    );
  }, [opacity]);

  const animatedStyle = useAnimatedStyle(() => ({ opacity: opacity.value }));

  return (
    <Animated.View
      className={cn('rounded-lg bg-surface-200 dark:bg-surface-800', className)}
      style={[animatedStyle, style]}
    />
  );
}

export function SkeletonCard({ className }: { className?: string }) {
  return (
    <View className={cn('rounded-2xl border border-surface-100 bg-white p-4 dark:border-surface-800 dark:bg-surface-900', className)}>
      <Skeleton className="mb-3 h-32 w-full rounded-xl" />
      <Skeleton className="mb-2 h-4 w-3/4" />
      <Skeleton className="h-4 w-1/2" />
    </View>
  );
}
