import React, { memo } from 'react';
import { Text, View } from 'react-native';

import { cn } from '@/shared/utils/cn';

export type StatTone = 'success' | 'warning' | 'danger' | 'neutral';

interface ToneStyle {
  readonly bg: string;
  readonly bar: string;
  readonly text: string;
}

const TONE_STYLES: Readonly<Record<StatTone, ToneStyle>> = {
  success: { bg: 'bg-success-50', bar: 'bg-success-500', text: 'text-success-800' },
  warning: { bg: 'bg-warning-50', bar: 'bg-warning-500', text: 'text-warning-800' },
  danger: { bg: 'bg-danger-50', bar: 'bg-danger-500', text: 'text-danger-800' },
  neutral: { bg: 'bg-surface-100 dark:bg-surface-800', bar: 'bg-surface-500', text: 'text-surface-900 dark:text-surface-50' },
};

interface Props {
  readonly label: string;
  readonly value: string;
  readonly tone?: StatTone;
  readonly className?: string;
}

function StatComponent({ label, value, tone = 'neutral', className }: Props) {
  const s = TONE_STYLES[tone];
  return (
    <View className={cn('flex-1 overflow-hidden rounded-lg', s.bg, className)}>
      <View className={cn('h-1 w-full', s.bar)} />
      <View className="p-2.5">
        <Text
          className="text-xs font-semibold uppercase tracking-wide text-surface-700 dark:text-surface-300"
          numberOfLines={1}
        >
          {label}
        </Text>
        <Text className={cn('mt-0.5 text-base font-extrabold', s.text)} numberOfLines={1}>
          {value}
        </Text>
      </View>
    </View>
  );
}

export const Stat = memo(StatComponent);
