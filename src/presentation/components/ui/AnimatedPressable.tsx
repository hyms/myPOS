import React, { useCallback } from 'react';
import { Pressable, StyleSheet, type PressableProps } from 'react-native';
import Reanimated, {
  useSharedValue,
  useAnimatedStyle,
  withTiming,
  Easing,
} from 'react-native-reanimated';

import { useReducedMotion } from '@/presentation/hooks/useReducedMotion';

const AnimatedReanimatedPressable = Reanimated.createAnimatedComponent(Pressable);

interface AnimatedPressableProps extends PressableProps {
  readonly scaleTo?: number;
  readonly opacityTo?: number;
  readonly duration?: number;
}

const DEFAULT_DURATION = 100;
const DEFAULT_SCALE = 0.96;
const DEFAULT_OPACITY = 0.7;
const INSTANT_DURATION = 0;

export function AnimatedPressable({
  scaleTo = DEFAULT_SCALE,
  opacityTo = DEFAULT_OPACITY,
  duration = DEFAULT_DURATION,
  onPressIn,
  onPressOut,
  style,
  children,
  ...rest
}: AnimatedPressableProps) {
  const reduced = useReducedMotion();
  const effectiveDuration = reduced ? INSTANT_DURATION : duration;
  const effectiveScale = reduced ? 1 : scaleTo;
  const effectiveOpacity = reduced ? 1 : opacityTo;
  const scale = useSharedValue(1);
  const opacity = useSharedValue(1);

  const animatedStyle = useAnimatedStyle(() => ({
    transform: [{ scale: scale.value }],
    opacity: opacity.value,
  }));

  const handlePressIn = useCallback(
    (e: unknown) => {
      scale.value = withTiming(effectiveScale, { duration: effectiveDuration, easing: Easing.out(Easing.quad) });
      opacity.value = withTiming(effectiveOpacity, { duration: effectiveDuration, easing: Easing.out(Easing.quad) });
      if (typeof onPressIn === 'function') {
        (onPressIn as (arg: unknown) => void)(e);
      }
    },
    [scale, opacity, effectiveScale, effectiveOpacity, effectiveDuration, onPressIn],
  );

  const handlePressOut = useCallback(
    (e: unknown) => {
      scale.value = withTiming(1, { duration: effectiveDuration, easing: Easing.out(Easing.quad) });
      opacity.value = withTiming(1, { duration: effectiveDuration, easing: Easing.out(Easing.quad) });
      if (typeof onPressOut === 'function') {
        (onPressOut as (arg: unknown) => void)(e);
      }
    },
    [scale, opacity, effectiveDuration, onPressOut],
  );

  return (
    <AnimatedReanimatedPressable
      onPressIn={handlePressIn}
      onPressOut={handlePressOut}
      style={
        typeof style === 'function'
          ? (state) => [animatedStyle, StyleSheet.flatten(style(state))]
          : [animatedStyle, StyleSheet.flatten(style)]
      }
      {...rest}
    >
      {children}
    </AnimatedReanimatedPressable>
  );
}
