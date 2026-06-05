import React, { memo } from 'react';
import { Text, View, type PressableProps, type StyleProp, type ViewStyle, ActivityIndicator } from 'react-native';

import { AnimatedPressable } from '@/presentation/components/ui/AnimatedPressable';
import { cn } from '@/shared/utils/cn';
import { DARK_PALETTE } from '@/presentation/theme/tokens';

type Variant = 'primary' | 'secondary' | 'danger' | 'ghost' | 'success';
type Size = 'sm' | 'md' | 'lg';

interface ButtonProps extends Omit<PressableProps, 'children' | 'style'> {
  readonly title: string;
  readonly variant?: Variant;
  readonly size?: Size;
  readonly fullWidth?: boolean;
  readonly leadingIcon?: React.ReactNode;
  readonly trailingIcon?: React.ReactNode;
  readonly className?: string;
  readonly style?: StyleProp<ViewStyle>;
  readonly busy?: boolean;
}

const VARIANT_BG: Record<Variant, string> = {
  primary: 'bg-accent',
  secondary: 'bg-surface-hi',
  danger: 'bg-danger',
  success: 'bg-success',
  ghost: 'bg-transparent',
};

const VARIANT_TEXT: Record<Variant, string> = {
  primary: 'text-white',
  secondary: 'text-ink-strong',
  danger: 'text-white',
  success: 'text-white',
  ghost: 'text-accent-bright',
};

const SPINNER_COLOR: Record<Variant, string> = {
  primary: '#ffffff',
  secondary: DARK_PALETTE.inkStrong,
  danger: '#ffffff',
  success: '#ffffff',
  ghost: DARK_PALETTE.accentBright,
};

const SIZE_CLASS: Record<Size, string> = {
  sm: 'px-3 py-2 rounded-md',
  md: 'px-4 py-3 rounded-lg',
  lg: 'px-5 py-4 rounded-xl',
};

const TEXT_SIZE: Record<Size, string> = {
  sm: 'text-sm',
  md: 'text-base',
  lg: 'text-lg',
};

function ButtonComponent({
  title,
  variant = 'primary',
  size = 'md',
  fullWidth,
  leadingIcon,
  trailingIcon,
  className,
  disabled,
  busy,
  ...rest
}: ButtonProps) {
  return (
    <AnimatedPressable
      accessibilityRole="button"
      accessibilityState={{ disabled: !!disabled || !!busy }}
      disabled={disabled || busy}
      className={cn(
        'flex-row items-center justify-center',
        SIZE_CLASS[size],
        VARIANT_BG[variant],
        fullWidth && 'w-full',
        (disabled || busy) && 'opacity-50',
        className,
      )}
      {...rest}
    >
      {busy ? (
        <ActivityIndicator size="small" color={SPINNER_COLOR[variant]} className="mr-2" />
      ) : leadingIcon ? (
        <View className="mr-2">{leadingIcon}</View>
      ) : null}
      <Text className={cn('font-semibold', TEXT_SIZE[size], VARIANT_TEXT[variant])}>{title}</Text>
      {!busy && trailingIcon ? <View className="ml-2">{trailingIcon}</View> : null}
    </AnimatedPressable>
  );
}

export const Button = memo(ButtonComponent);
