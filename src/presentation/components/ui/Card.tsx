import React, { memo } from 'react';
import { Pressable, Text, View } from 'react-native';

import { cn } from '@/shared/utils/cn';

interface CardProps {
  readonly children: React.ReactNode;
  readonly onPress?: () => void;
  readonly className?: string;
}

function CardComponent({ children, onPress, className }: CardProps) {
  const inner = (
    <View
      className={cn(
        'rounded-2xl bg-white p-4 shadow-sm dark:bg-surface-900',
        'border border-surface-100 dark:border-surface-800',
        className,
      )}
    >
      {children}
    </View>
  );
  if (!onPress) return inner;
  return (
    <Pressable
      onPress={onPress}
      android_ripple={{ color: '#fef3c7', borderless: false }}
      accessibilityRole="button"
    >
      {inner}
    </Pressable>
  );
}

export const Card = memo(CardComponent);

interface BadgeProps {
  readonly label: string;
  readonly tone?: 'neutral' | 'success' | 'warning' | 'danger' | 'primary';
  readonly className?: string;
}

const TONE_BG: Record<NonNullable<BadgeProps['tone']>, string> = {
  neutral: 'bg-surface-200 dark:bg-surface-800',
  success: 'bg-success-50',
  warning: 'bg-warning-50',
  danger: 'bg-danger-50',
  primary: 'bg-primary-100',
};

const TONE_TEXT: Record<NonNullable<BadgeProps['tone']>, string> = {
  neutral: 'text-surface-700 dark:text-surface-200',
  success: 'text-success-700',
  warning: 'text-warning-600',
  danger: 'text-danger-700',
  primary: 'text-primary-800',
};

export function Badge({ label, tone = 'neutral', className }: BadgeProps) {
  return (
    <View className={cn('self-start rounded-full px-2 py-1', TONE_BG[tone], className)}>
      <Text className={cn('text-xs font-semibold', TONE_TEXT[tone])}>{label}</Text>
    </View>
  );
}
