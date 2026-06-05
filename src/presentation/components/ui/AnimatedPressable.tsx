import React, { useCallback } from 'react';
import { Pressable, type PressableProps } from 'react-native';
import Animated, {
  useSharedValue,
  useAnimatedStyle,
  withTiming,
  Easing,
} from 'react-native-reanimated';

interface AnimatedPressableProps extends PressableProps {
  readonly scaleTo?: number;
  readonly opacityTo?: number;
  readonly duration?: number;
}

const DEFAULT_DURATION = 100;
const DEFAULT_SCALE = 0.96;
const DEFAULT_OPACITY = 0.7;

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
  const scale = useSharedValue(1);
  const opacity = useSharedValue(1);

  const animatedStyle = useAnimatedStyle(() => ({
    transform: [{ scale: scale.value }],
    opacity: opacity.value,
  }));

  const handlePressIn = useCallback(
    (e: any) => {
      scale.value = withTiming(scaleTo, { duration, easing: Easing.out(Easing.quad) });
      opacity.value = withTiming(opacityTo, { duration, easing: Easing.out(Easing.quad) });
      onPressIn?.(e);
    },
    [scale, opacity, scaleTo, opacityTo, duration, onPressIn],
  );

  const handlePressOut = useCallback(
    (e: any) => {
      scale.value = withTiming(1, { duration, easing: Easing.out(Easing.quad) });
      opacity.value = withTiming(1, { duration, easing: Easing.out(Easing.quad) });
      onPressOut?.(e);
    },
    [scale, opacity, duration, onPressOut],
  );

  return (
    <Animated.View style={[animatedStyle, typeof style !== 'function' ? style : undefined]}>
      <Pressable onPressIn={handlePressIn} onPressOut={handlePressOut} {...rest}>
        {children}
      </Pressable>
    </Animated.View>
  );
}
