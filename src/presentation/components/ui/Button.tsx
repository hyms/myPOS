import React, { memo } from 'react';
import { Pressable, Text, View, type PressableProps, type StyleProp, type ViewStyle } from 'react-native';

import { cn } from '@/shared/utils/cn';

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
  primary: 'bg-primary-600 active:bg-primary-700',
  secondary: 'bg-surface-200 active:bg-surface-300 dark:bg-surface-800 dark:active:bg-surface-700',
  danger: 'bg-danger-600 active:bg-danger-700',
  success: 'bg-success-600 active:bg-success-700',
  ghost: 'bg-transparent',
};

const VARIANT_TEXT: Record<Variant, string> = {
  primary: 'text-white',
  secondary: 'text-surface-900 dark:text-surface-50',
  danger: 'text-white',
  success: 'text-white',
  ghost: 'text-primary-600',
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
    <Pressable
      accessibilityRole="button"
      accessibilityState={{ disabled: !!disabled }}
      disabled={disabled}
      className={cn(
        'flex-row items-center justify-center',
        SIZE_CLASS[size],
        VARIANT_BG[variant],
        fullWidth && 'w-full',
        disabled && 'opacity-50',
        className,
      )}
      {...rest}
    >
      {leadingIcon ? <View className="mr-2">{leadingIcon}</View> : null}
      <Text className={cn('font-semibold', TEXT_SIZE[size], VARIANT_TEXT[variant])}>{title}</Text>
      {trailingIcon ? <View className="ml-2">{trailingIcon}</View> : null}
    </Pressable>
  );
}

export const Button = memo(ButtonComponent);
ButtonComponent.displayName = 'Button';
