import React, { useEffect } from 'react';
import { View, type ViewStyle } from 'react-native';
import Animated, {
  useSharedValue,
  useAnimatedStyle,
  withRepeat,
  withTiming,
  Easing,
} from 'react-native-reanimated';

import { useReducedMotion } from '@/presentation/hooks/useReducedMotion';
import { cn } from '@/shared/utils/cn';

interface SkeletonProps {
  readonly className?: string;
  readonly style?: ViewStyle;
}

const MIN_OPACITY = 0.3;
const MAX_OPACITY = 1;
const PULSE_DURATION = 800;

export function Skeleton({ className, style }: SkeletonProps) {
  const reduced = useReducedMotion();
  const opacity = useSharedValue(reduced ? MAX_OPACITY : MIN_OPACITY);

  useEffect(() => {
    if (reduced) {
      opacity.value = MAX_OPACITY;
      return;
    }
    opacity.value = withRepeat(
      withTiming(MAX_OPACITY, { duration: PULSE_DURATION, easing: Easing.inOut(Easing.quad) }),
      -1,
      true,
    );
  }, [opacity, reduced]);

  const animatedStyle = useAnimatedStyle(() => ({ opacity: opacity.value }));

  return (
    <Animated.View
      accessibilityElementsHidden
      importantForAccessibility="no"
      className={cn('rounded-lg bg-surface-hi', className)}
      style={[animatedStyle, style]}
    />
  );
}

export function SkeletonCard({ className }: { className?: string }) {
  return (
    <View className={cn('rounded-2xl border border-border bg-surface p-4', className)}>
      <Skeleton className="mb-3 h-32 w-full rounded-xl" />
      <Skeleton className="mb-2 h-4 w-3/4" />
      <Skeleton className="h-4 w-1/2" />
    </View>
  );
}

interface ListFooterLoaderProps {
  readonly count?: number;
  readonly itemHeight?: number;
  readonly className?: string;
}

export function ListFooterLoader({ count = 3, itemHeight = 72, className }: ListFooterLoaderProps) {
  return (
    <View accessibilityLabel="Cargando más elementos" className={cn('gap-2 p-3', className)}>
      {Array.from({ length: count }).map((_, i) => (
        <Skeleton key={i} className="rounded-2xl" style={{ height: itemHeight }} />
      ))}
    </View>
  );
}

interface ListCountProps {
  readonly count: number;
  readonly hasMore: boolean;
  readonly page: number;
  readonly pageSize: number;
  readonly label?: string;
  readonly className?: string;
}

export function ListCountText({ count, hasMore, page, pageSize, label = 'elementos', className }: ListCountProps) {
  const text = `Mostrando ${count}${hasMore ? '+' : ''} · página ${page + 1} · lote ${pageSize} ${label}`;
  return (
    <View accessibilityRole="text" accessibilityLabel={text} className={cn('px-3 pb-2', className)}>
      <View className="h-px bg-surface-hi" />
      <View className="flex-row items-center justify-between pt-2">
        <View className="h-1.5 w-24 rounded-full bg-surface-hi" />
        {hasMore ? (
          <View className="h-1.5 w-3 rounded-full bg-accent" />
        ) : null}
      </View>
    </View>
  );
}
