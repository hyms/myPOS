import React, { memo } from 'react';
import { Pressable, Text, View } from 'react-native';

import { cn } from '@/shared/utils/cn';
import { DARK_PALETTE } from '@/presentation/theme/tokens';

interface CardProps {
  readonly children: React.ReactNode;
  readonly onPress?: () => void;
  readonly className?: string;
}

function CardComponent({ children, onPress, className }: CardProps) {
  const inner = (
    <View
      className={cn(
        'rounded-2xl bg-surface p-4 shadow-sm border border-border-subtle',
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
      android_ripple={{ color: DARK_PALETTE.borderSubtle, borderless: false }}
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
  neutral: 'bg-surface-hi',
  success: 'bg-success-soft',
  warning: 'bg-warning-soft',
  danger: 'bg-danger-soft',
  primary: 'bg-accent',
};

const TONE_TEXT: Record<NonNullable<BadgeProps['tone']>, string> = {
  neutral: 'text-ink',
  success: 'text-success',
  warning: 'text-warning',
  danger: 'text-danger',
  primary: 'text-ink',
};

export function Badge({ label, tone = 'neutral', className }: BadgeProps) {
  return (
    <View className={cn('self-start rounded-full px-2 py-1', TONE_BG[tone], className)}>
      <Text className={cn('text-xs font-semibold', TONE_TEXT[tone])}>{label}</Text>
    </View>
  );
}
